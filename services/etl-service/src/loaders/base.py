"""Base types and abstractions for database loaders."""

from __future__ import annotations

from abc import ABC, abstractmethod
from dataclasses import dataclass
from typing import Dict, Generic, Optional, Tuple, TypeVar

import pandas as pd

T = TypeVar("T")


@dataclass(frozen=True)
class UpsertResult:
    """Immutable result of a bulk upsert operation."""

    inserted: int = 0
    updated: int = 0
    failed: int = 0
    skipped: int = 0
    total_batches: int = 0

    def merge(self, other: UpsertResult) -> UpsertResult:
        return UpsertResult(
            inserted=self.inserted + other.inserted,
            updated=self.updated + other.updated,
            failed=self.failed + other.failed,
            skipped=self.skipped + other.skipped,
            total_batches=self.total_batches + other.total_batches,
        )

    def to_dict(self) -> Dict[str, int]:
        return {
            "inserted": self.inserted,
            "updated": self.updated,
            "failed": self.failed,
            "skipped": self.skipped,
            "total_batches": self.total_batches,
        }


@dataclass(frozen=True)
class TableSchema:
    """Defines table structure for generic upsert operations."""

    table_name: str
    columns: Tuple[str, ...]
    conflict_column: str
    update_columns: Tuple[str, ...]
    timestamp_columns: Tuple[str, ...] = ("createdAt", "updatedAt")
    template: Optional[str] = None


@dataclass
class EntityMapper(ABC, Generic[T]):
    """Abstract mapper that transforms DataFrame rows to database tuples."""

    @abstractmethod
    def validate(self, row: pd.Series) -> bool:
        """Return True if row has all required fields."""
        ...

    @abstractmethod
    def to_record(self, row: pd.Series) -> T:
        """Transform a row into a database-ready tuple."""
        ...
