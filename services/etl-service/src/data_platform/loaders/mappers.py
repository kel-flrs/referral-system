"""Entity mappers and schema definitions."""

from __future__ import annotations

import json
from dataclasses import dataclass
from typing import Any, Tuple

import pandas as pd
from cuid import cuid

from .base import EntityMapper, TableSchema


@dataclass
class ConsultantMapper(EntityMapper[Tuple]):
    """Maps consultant data from DataFrame to database records."""

    required_fields: Tuple[str, ...] = ("bullhornId", "firstName", "lastName", "email")

    def validate(self, row: pd.Series) -> bool:
        return all(row.get(f) for f in self.required_fields)

    def to_record(self, row: pd.Series) -> Tuple:
        return (
            cuid(),
            row["bullhornId"],
            self._clean_str(row["firstName"]),
            self._clean_str(row["lastName"]),
            self._clean_str(row["email"]),
            row.get("phone"),
            True,
        )

    @staticmethod
    def _clean_str(val: Any) -> Any:
        return val.strip() if isinstance(val, str) else val


@dataclass
class CandidateMapper(EntityMapper[Tuple]):
    """Maps candidate data from DataFrame to database records."""

    required_fields: Tuple[str, ...] = ("bullhornId", "firstName", "lastName", "email")

    def validate(self, row: pd.Series) -> bool:
        return all(row.get(f) for f in self.required_fields)

    def to_record(self, row: pd.Series) -> Tuple:
        experience = row.get("experience")
        education = row.get("education")
        return (
            cuid(),
            row["bullhornId"],
            self._clean_str(row["firstName"]),
            self._clean_str(row["lastName"]),
            self._clean_str(row["email"]),
            row.get("phone"),
            row.get("skills", []),
            json.dumps(experience) if experience else None,
            json.dumps(education) if education else None,
            row.get("location"),
            "ACTIVE",
        )

    @staticmethod
    def _clean_str(val: Any) -> Any:
        return val.strip() if isinstance(val, str) else val


# Pre-defined schemas
CONSULTANT_SCHEMA = TableSchema(
    table_name='"Consultant"',
    columns=("id", "bullhornId", "firstName", "lastName", "email", "phone", "isActive", "createdAt", "updatedAt"),
    conflict_column="bullhornId",
    update_columns=("firstName", "lastName", "email", "phone", "isActive"),
    template="(%s, %s, %s, %s, %s, %s, %s, NOW(), NOW())",
)

CANDIDATE_SCHEMA = TableSchema(
    table_name='"Candidate"',
    columns=(
        "id", "bullhornId", "firstName", "lastName", "email", "phone", "skills",
        "experience", "education", "location", "status", "lastSyncedAt", "createdAt", "updatedAt",
    ),
    conflict_column="bullhornId",
    update_columns=(
        "firstName", "lastName", "email", "phone", "skills", "experience",
        "education", "location", "status", "lastSyncedAt",
    ),
    template="(%s, %s, %s, %s, %s, %s, %s, %s::jsonb, %s::jsonb, %s, %s, NOW(), NOW(), NOW())",
)


@dataclass
class PositionMapper(EntityMapper[Tuple]):
    """Maps position/job order data from DataFrame to database records."""

    required_fields: Tuple[str, ...] = ("bullhornId", "title")

    def validate(self, row: pd.Series) -> bool:
        return all(row.get(f) for f in self.required_fields)

    def to_record(self, row: pd.Series) -> Tuple:
        return (
            cuid(),
            row["bullhornId"],
            self._clean_str(row["title"]),
            row.get("description"),
            row.get("employmentType"),
            row.get("requiredSkills", []),
            row.get("preferredSkills", []),
            row.get("experienceLevel"),
            row.get("location"),
            row.get("salary"),
            row.get("clientName"),
            row.get("clientBullhornId"),
            row.get("status", "OPEN"),
            row.get("openDate"),
            row.get("closeDate"),
        )

    @staticmethod
    def _clean_str(val: Any) -> Any:
        return val.strip() if isinstance(val, str) else val


POSITION_SCHEMA = TableSchema(
    table_name='"Position"',
    columns=(
        "id", "bullhornId", "title", "description", "employmentType", "requiredSkills",
        "preferredSkills", "experienceLevel", "location", "salary", "clientName",
        "clientBullhornId", "status", "openDate", "closeDate", "lastSyncedAt", "createdAt", "updatedAt",
    ),
    conflict_column="bullhornId",
    update_columns=(
        "title", "description", "employmentType", "requiredSkills", "preferredSkills",
        "experienceLevel", "location", "salary", "clientName", "clientBullhornId",
        "status", "openDate", "closeDate", "lastSyncedAt",
    ),
    template="(%s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, NOW(), NOW(), NOW())",
)
