"""Pipeline run logging and statistics."""

from __future__ import annotations

import json
import uuid
from typing import Any, Dict, Optional

from data_platform.utils.database import db
from data_platform.utils.logger import get_logger

logger = get_logger(__name__)


class PipelineRunLogger:
    """
    Pipeline run logging and statistics.

    Note: The pipeline_runs table should be created via migrations, not at runtime.
    This class assumes the table exists.
    """

    def log_run(
        self,
        pipeline_name: str,
        status: str,
        records_processed: int,
        records_inserted: int = 0,
        records_updated: int = 0,
        error_message: Optional[str] = None,
        metadata: Optional[Dict] = None,
    ) -> None:
        """Log a pipeline run."""
        duration = metadata.get("duration_seconds") if metadata and status in ("SUCCESS", "FAILED") else None

        with db.get_cursor() as cursor:
            cursor.execute(
                """
                INSERT INTO pipeline_runs (
                    id, pipeline_name, status, records_processed, records_inserted,
                    records_updated, error_message, metadata, started_at, completed_at, duration_seconds
                ) VALUES (%s, %s, %s, %s, %s, %s, %s, %s, NOW() - INTERVAL '%s seconds', NOW(), %s)
                """,
                (
                    str(uuid.uuid4()),
                    pipeline_name,
                    status,
                    records_processed,
                    records_inserted,
                    records_updated,
                    error_message,
                    json.dumps(metadata) if metadata else None,
                    duration or 0,
                    duration,
                ),
            )

        logger.info("Pipeline run logged", pipeline=pipeline_name, status=status, processed=records_processed)

    def get_stats(self, pipeline_name: str, days: int = 7) -> Dict[str, Any]:
        """Get pipeline statistics for the last N days."""
        with db.get_cursor() as cursor:
            cursor.execute(
                """
                SELECT
                    COUNT(*) as total_runs,
                    COUNT(*) FILTER (WHERE status = 'SUCCESS') as successful_runs,
                    COUNT(*) FILTER (WHERE status = 'FAILED') as failed_runs,
                    AVG(duration_seconds) as avg_duration_seconds,
                    MAX(duration_seconds) as max_duration_seconds,
                    SUM(records_processed) as total_records_processed
                FROM pipeline_runs
                WHERE pipeline_name = %s
                  AND started_at > NOW() - make_interval(days => %s)
                """,
                (pipeline_name, days),
            )
            row = cursor.fetchone()

        if not row or row["total_runs"] == 0:
            return {
                "total_runs": 0,
                "successful_runs": 0,
                "failed_runs": 0,
                "success_rate": 0.0,
                "avg_duration_seconds": 0.0,
                "max_duration_seconds": 0,
                "total_records_processed": 0,
            }

        return {
            "total_runs": row["total_runs"],
            "successful_runs": row["successful_runs"],
            "failed_runs": row["failed_runs"],
            "success_rate": round(row["successful_runs"] / row["total_runs"] * 100, 2),
            "avg_duration_seconds": round(float(row["avg_duration_seconds"] or 0), 2),
            "max_duration_seconds": row["max_duration_seconds"],
            "total_records_processed": row["total_records_processed"] or 0,
        }
