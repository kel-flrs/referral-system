"""Production-grade bulk database loader."""

from __future__ import annotations

import time
from typing import Dict, Iterable, List, Tuple

import pandas as pd
import psycopg2
import psycopg2.extras
from tenacity import retry, retry_if_exception_type, stop_after_attempt, wait_exponential

from data_platform.utils.database import db
from data_platform.utils.logger import get_logger

from .base import EntityMapper, TableSchema, UpsertResult
from .mappers import (
    CANDIDATE_SCHEMA,
    CONSULTANT_SCHEMA,
    POSITION_SCHEMA,
    CandidateMapper,
    ConsultantMapper,
    PositionMapper,
)

logger = get_logger(__name__)


class BulkDatabaseLoader:
    """
    Production-grade bulk loader with:
    - Generic upsert supporting any entity type via schema + mapper pattern
    - Retry logic with exponential backoff
    - Per-batch error isolation via savepoints
    - Accurate insert/update counting via xmax
    - Streaming batches for memory efficiency
    """

    RETRYABLE_ERRORS = (psycopg2.OperationalError, psycopg2.InterfaceError)

    def __init__(self, batch_size: int = 1000, max_retries: int = 3):
        self.engine = db.get_engine()
        self.batch_size = batch_size
        self.max_retries = max_retries
        logger.info("BulkDatabaseLoader initialized", batch_size=batch_size, max_retries=max_retries)

    def _stream_batches(self, df: pd.DataFrame) -> Iterable[pd.DataFrame]:
        """Yield DataFrames in chunks to limit memory usage."""
        for start in range(0, len(df), self.batch_size):
            yield df.iloc[start : start + self.batch_size]

    @staticmethod
    def _parse_xmax_flags(status_flags: List[Tuple[int]]) -> Tuple[int, int]:
        """Parse PostgreSQL xmax to distinguish inserts (xmax=0) from updates."""
        inserted = sum(1 for (flag,) in status_flags if flag == 0)
        return inserted, len(status_flags) - inserted

    def _build_upsert_sql(self, schema: TableSchema) -> str:
        """Dynamically build upsert SQL from schema definition."""
        cols = ", ".join(f'"{c}"' for c in schema.columns)
        conflict = f'"{schema.conflict_column}"'
        updates = ", ".join(
            f'"{c}" = EXCLUDED."{c}"' if c != "updatedAt" else f'"{c}" = NOW()'
            for c in schema.update_columns
        )
        if "updatedAt" not in schema.update_columns:
            updates += ', "updatedAt" = NOW()'

        return f"""
            INSERT INTO {schema.table_name} ({cols}) VALUES %s
            ON CONFLICT ({conflict})
            DO UPDATE SET {updates}
            RETURNING xmax
        """

    def _execute_batch_with_retry(
        self,
        cursor,
        sql: str,
        records: List[Tuple],
        template: str,
        savepoint: str,
    ) -> Tuple[int, int]:
        """Execute a single batch with retry logic and savepoint isolation."""

        @retry(
            stop=stop_after_attempt(self.max_retries),
            wait=wait_exponential(multiplier=0.5, min=0.5, max=10),
            retry=retry_if_exception_type(self.RETRYABLE_ERRORS),
            reraise=True,
        )
        def _do_execute():
            cursor.execute(f"SAVEPOINT {savepoint}")
            try:
                status_flags = psycopg2.extras.execute_values(
                    cursor, sql, records, template=template, fetch=True
                )
                cursor.execute(f"RELEASE SAVEPOINT {savepoint}")
                return status_flags
            except Exception:
                cursor.execute(f"ROLLBACK TO SAVEPOINT {savepoint}")
                raise

        status_flags = _do_execute()
        return self._parse_xmax_flags(status_flags)

    def bulk_upsert(
        self,
        df: pd.DataFrame,
        schema: TableSchema,
        mapper: EntityMapper,
        entity_name: str = "record",
    ) -> UpsertResult:
        """
        Generic bulk upsert for any entity type.

        Args:
            df: Source DataFrame
            schema: Table schema definition
            mapper: Entity mapper for validation and transformation
            entity_name: Human-readable name for logging

        Returns:
            UpsertResult with counts
        """
        logger.info(f"Starting bulk {entity_name} upsert", total_records=len(df))

        if df.empty:
            logger.warning(f"No {entity_name}s to upsert")
            return UpsertResult()

        sql = self._build_upsert_sql(schema)
        result = UpsertResult()

        with db.get_cursor() as cursor:
            for batch_num, batch_df in enumerate(self._stream_batches(df), start=1):
                records = []
                skipped = 0

                for _, row in batch_df.iterrows():
                    if not mapper.validate(row):
                        skipped += 1
                        continue
                    records.append(mapper.to_record(row))

                if not records:
                    result = result.merge(UpsertResult(skipped=skipped))
                    continue

                start_time = time.perf_counter()
                try:
                    inserted, updated = self._execute_batch_with_retry(
                        cursor,
                        sql,
                        records,
                        schema.template,
                        savepoint=f"bulk_{entity_name}_{batch_num}",
                    )
                    duration = time.perf_counter() - start_time

                    batch_result = UpsertResult(
                        inserted=inserted,
                        updated=updated,
                        skipped=skipped,
                        total_batches=1,
                    )
                    logger.info(
                        f"{entity_name.capitalize()} batch completed",
                        batch_num=batch_num,
                        batch_size=len(records),
                        inserted=inserted,
                        updated=updated,
                        duration_seconds=round(duration, 3),
                        rows_per_sec=round(len(records) / duration, 1) if duration else None,
                    )

                except Exception as exc:
                    logger.error(
                        f"{entity_name.capitalize()} batch failed after retries",
                        error=str(exc),
                        batch_num=batch_num,
                        batch_size=len(records),
                    )
                    batch_result = UpsertResult(failed=len(records), skipped=skipped)

                result = result.merge(batch_result)

        logger.info(
            f"Bulk {entity_name} upsert complete",
            **result.to_dict(),
            total_input_records=len(df),
        )
        return result

    def upsert_consultants_bulk(self, df: pd.DataFrame) -> Dict[str, int]:
        """Bulk upsert consultants."""
        return self.bulk_upsert(df, CONSULTANT_SCHEMA, ConsultantMapper(), "consultant").to_dict()

    def upsert_candidates_bulk(self, df: pd.DataFrame) -> Dict[str, int]:
        """Bulk upsert candidates."""
        return self.bulk_upsert(df, CANDIDATE_SCHEMA, CandidateMapper(), "candidate").to_dict()

    def upsert_positions_bulk(self, df: pd.DataFrame) -> Dict[str, int]:
        """Bulk upsert positions."""
        return self.bulk_upsert(df, POSITION_SCHEMA, PositionMapper(), "position").to_dict()
