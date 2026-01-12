"""Data quality validation for ETL pipelines."""

from typing import Dict, List, Tuple
from datetime import datetime

import pandas as pd

from data_platform.utils.logger import get_logger

logger = get_logger(__name__)


class DataQualityValidator:
    """Validates data quality for ETL pipelines."""

    def __init__(self):
        self.validation_results = []

    def validate_consultants(self, df: pd.DataFrame) -> Tuple[bool, List[Dict]]:
        """
        Validate consultant data quality.

        Args:
            df: DataFrame with consultant data

        Returns:
            Tuple of (is_valid, list of validation errors)
        """
        logger.info("Validating consultant data quality", count=len(df))
        errors = []

        # Check required fields
        required_fields = ['bullhornId', 'firstName', 'lastName', 'email']
        for field in required_fields:
            if field not in df.columns:
                errors.append({
                    'rule': 'required_field',
                    'field': field,
                    'message': f'Required field {field} is missing'
                })
                continue

            null_count = df[field].isnull().sum()
            if null_count > 0:
                errors.append({
                    'rule': 'null_values',
                    'field': field,
                    'count': int(null_count),
                    'message': f'{null_count} null values found in required field {field}'
                })

        # Check email format
        if 'email' in df.columns:
            invalid_emails = df[~df['email'].str.contains('@', na=False)]
            if len(invalid_emails) > 0:
                errors.append({
                    'rule': 'email_format',
                    'field': 'email',
                    'count': len(invalid_emails),
                    'message': f'{len(invalid_emails)} invalid email formats found'
                })

        # Check for duplicate bullhornIds
        if 'bullhornId' in df.columns:
            duplicates = df[df.duplicated(subset=['bullhornId'], keep=False)]
            if len(duplicates) > 0:
                errors.append({
                    'rule': 'duplicate_ids',
                    'field': 'bullhornId',
                    'count': len(duplicates),
                    'message': f'{len(duplicates)} duplicate bullhornIds found'
                })

        # Check skills field is array
        if 'skills' in df.columns:
            non_array_skills = df[~df['skills'].apply(lambda x: isinstance(x, list) or pd.isna(x))]
            if len(non_array_skills) > 0:
                errors.append({
                    'rule': 'data_type',
                    'field': 'skills',
                    'count': len(non_array_skills),
                    'message': f'{len(non_array_skills)} non-array skills values found'
                })

        # Check availability values
        if 'availability' in df.columns:
            valid_availability = ['AVAILABLE', 'BUSY', 'UNAVAILABLE']
            invalid_availability = df[~df['availability'].isin(valid_availability) & df['availability'].notna()]
            if len(invalid_availability) > 0:
                errors.append({
                    'rule': 'enum_values',
                    'field': 'availability',
                    'count': len(invalid_availability),
                    'message': f'{len(invalid_availability)} invalid availability values found'
                })

        is_valid = len(errors) == 0
        logger.info(
            "Consultant data validation complete",
            is_valid=is_valid,
            error_count=len(errors)
        )

        return is_valid, errors

    def validate_candidates(self, df: pd.DataFrame) -> Tuple[bool, List[Dict]]:
        """
        Validate candidate data quality.

        Args:
            df: DataFrame with candidate data

        Returns:
            Tuple of (is_valid, list of validation errors)
        """
        logger.info("Validating candidate data quality", count=len(df))
        errors = []

        # Check required fields
        required_fields = ['bullhornId', 'firstName', 'lastName', 'email']
        for field in required_fields:
            if field not in df.columns:
                errors.append({
                    'rule': 'required_field',
                    'field': field,
                    'message': f'Required field {field} is missing'
                })
                continue

            null_count = df[field].isnull().sum()
            if null_count > 0:
                errors.append({
                    'rule': 'null_values',
                    'field': field,
                    'count': int(null_count),
                    'message': f'{null_count} null values found in required field {field}'
                })

        # Check email format
        if 'email' in df.columns:
            invalid_emails = df[~df['email'].str.contains('@', na=False)]
            if len(invalid_emails) > 0:
                errors.append({
                    'rule': 'email_format',
                    'field': 'email',
                    'count': len(invalid_emails),
                    'message': f'{len(invalid_emails)} invalid email formats found'
                })

        # Check for duplicate bullhornIds
        if 'bullhornId' in df.columns:
            duplicates = df[df.duplicated(subset=['bullhornId'], keep=False)]
            if len(duplicates) > 0:
                errors.append({
                    'rule': 'duplicate_ids',
                    'field': 'bullhornId',
                    'count': len(duplicates),
                    'message': f'{len(duplicates)} duplicate bullhornIds found'
                })

        # Check array fields
        array_fields = ['skills', 'education', 'certifications', 'preferredLocations']
        for field in array_fields:
            if field in df.columns:
                non_array = df[~df[field].apply(lambda x: isinstance(x, list) or pd.isna(x))]
                if len(non_array) > 0:
                    errors.append({
                        'rule': 'data_type',
                        'field': field,
                        'count': len(non_array),
                        'message': f'{len(non_array)} non-array {field} values found'
                    })

        is_valid = len(errors) == 0
        logger.info(
            "Candidate data validation complete",
            is_valid=is_valid,
            error_count=len(errors)
        )

        return is_valid, errors

    def validate_data_freshness(self, last_sync_time: datetime, max_age_hours: int = 24) -> Tuple[bool, str]:
        """
        Validate data freshness.

        Args:
            last_sync_time: Last sync timestamp
            max_age_hours: Maximum allowed age in hours

        Returns:
            Tuple of (is_fresh, message)
        """
        age_hours = (datetime.now() - last_sync_time).total_seconds() / 3600

        if age_hours > max_age_hours:
            return False, f"Data is {age_hours:.1f} hours old, exceeds maximum of {max_age_hours} hours"

        return True, f"Data is {age_hours:.1f} hours old, within acceptable range"

    def generate_quality_report(self, validation_results: List[Dict]) -> str:
        """
        Generate a human-readable quality report.

        Args:
            validation_results: List of validation results

        Returns:
            Formatted quality report
        """
        report = ["Data Quality Report", "=" * 50, ""]

        total_errors = sum(len(result.get('errors', [])) for result in validation_results)

        if total_errors == 0:
            report.append("✓ All data quality checks passed!")
        else:
            report.append(f"✗ {total_errors} data quality issues found:")
            report.append("")

            for result in validation_results:
                dataset = result.get('dataset', 'Unknown')
                errors = result.get('errors', [])

                if errors:
                    report.append(f"{dataset}:")
                    for error in errors:
                        report.append(f"  - {error['message']}")
                    report.append("")

        return "\n".join(report)
