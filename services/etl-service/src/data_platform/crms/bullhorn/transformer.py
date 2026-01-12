"""Transforms raw Bullhorn data into database-ready format."""

from typing import Dict, List, Optional
from datetime import datetime

import pandas as pd

from data_platform.utils.logger import get_logger

logger = get_logger(__name__)


class BullhornTransformer:
    """Handles data transformation for Bullhorn data."""

    @staticmethod
    def transform_consultant(raw_consultant: Dict) -> Dict:
        """
        Transform a single consultant record.

        Args:
            raw_consultant: Raw consultant data from Bullhorn

        Returns:
            Transformed consultant data ready for database
        """
        return {
            'bullhornId': raw_consultant.get('bullhornId') or str(raw_consultant.get('id', '')),
            'firstName': raw_consultant.get('firstName', ''),
            'lastName': raw_consultant.get('lastName', ''),
            'email': raw_consultant.get('email', ''),
            'phone': raw_consultant.get('phone'),
            'skills': raw_consultant.get('skills', []),
            'experience': raw_consultant.get('experience'),
            'certifications': raw_consultant.get('certifications', []),
            'availability': raw_consultant.get('availability', 'AVAILABLE'),
            'location': raw_consultant.get('location'),
            'hourlyRate': raw_consultant.get('hourlyRate'),
            'resumeUrl': raw_consultant.get('resumeUrl'),
            'linkedinUrl': raw_consultant.get('linkedinUrl'),
            'notes': raw_consultant.get('notes'),
        }

    @staticmethod
    def transform_candidate(raw_candidate: Dict) -> Dict:
        """
        Transform a single candidate record.

        Args:
            raw_candidate: Raw candidate data from Bullhorn

        Returns:
            Transformed candidate data ready for database
        """
        return {
            'bullhornId': raw_candidate.get('bullhornId') or str(raw_candidate.get('id', '')),
            'firstName': raw_candidate.get('firstName', ''),
            'lastName': raw_candidate.get('lastName', ''),
            'email': raw_candidate.get('email', ''),
            'phone': raw_candidate.get('phone'),
            'skills': raw_candidate.get('skills', []),
            'experience': raw_candidate.get('experience'),
            'education': raw_candidate.get('education', []),
            'certifications': raw_candidate.get('certifications', []),
            'desiredSalary': raw_candidate.get('desiredSalary'),
            'noticePeriod': raw_candidate.get('noticePeriod'),
            'preferredLocations': raw_candidate.get('preferredLocations', []),
            'resumeUrl': raw_candidate.get('resumeUrl'),
            'linkedinUrl': raw_candidate.get('linkedinUrl'),
            'githubUrl': raw_candidate.get('githubUrl'),
            'portfolioUrl': raw_candidate.get('portfolioUrl'),
            'notes': raw_candidate.get('notes'),
        }

    @staticmethod
    def transform_position(raw_position: Dict) -> Dict:
        """
        Transform a single position/job order record.

        Args:
            raw_position: Raw position data from Bullhorn

        Returns:
            Transformed position data ready for database
        """
        return {
            'bullhornId': raw_position.get('bullhornId') or str(raw_position.get('id', '')),
            'title': raw_position.get('title', ''),
            'description': raw_position.get('description'),
            'employmentType': raw_position.get('employmentType'),
            'requiredSkills': raw_position.get('requiredSkills', []),
            'preferredSkills': raw_position.get('preferredSkills', []),
            'experienceLevel': raw_position.get('experienceLevel'),
            'location': raw_position.get('location'),
            'salary': str(raw_position.get('salary', '')) if raw_position.get('salary') else None,
            'clientName': raw_position.get('clientName'),
            'clientBullhornId': raw_position.get('clientBullhornId'),
            'status': raw_position.get('status', 'OPEN'),
            'openDate': raw_position.get('openDate'),
            'closeDate': raw_position.get('closeDate'),
        }

    def transform_consultants(self, raw_consultants: List[Dict]) -> pd.DataFrame:
        """
        Transform multiple consultant records into a DataFrame.

        Args:
            raw_consultants: List of raw consultant records

        Returns:
            DataFrame with transformed consultant data
        """
        logger.info("Transforming consultants", count=len(raw_consultants))

        if not raw_consultants:
            logger.warning("No consultants to transform")
            return pd.DataFrame()

        transformed = [self.transform_consultant(c) for c in raw_consultants]
        df = pd.DataFrame(transformed)

        # Data quality transformations
        df['email'] = df['email'].str.lower().str.strip()
        df['firstName'] = df['firstName'].str.strip()
        df['lastName'] = df['lastName'].str.strip()

        # Remove duplicates based on bullhornId
        df = df.drop_duplicates(subset=['bullhornId'], keep='last')

        logger.info("Consultants transformed", final_count=len(df))
        return df

    def transform_candidates(self, raw_candidates: List[Dict]) -> pd.DataFrame:
        """
        Transform multiple candidate records into a DataFrame.

        Args:
            raw_candidates: List of raw candidate records

        Returns:
            DataFrame with transformed candidate data
        """
        logger.info("Transforming candidates", count=len(raw_candidates))

        if not raw_candidates:
            logger.warning("No candidates to transform")
            return pd.DataFrame()

        transformed = [self.transform_candidate(c) for c in raw_candidates]
        df = pd.DataFrame(transformed)

        # Data quality transformations
        df['email'] = df['email'].str.lower().str.strip()
        df['firstName'] = df['firstName'].str.strip()
        df['lastName'] = df['lastName'].str.strip()

        # Remove duplicates based on bullhornId
        df = df.drop_duplicates(subset=['bullhornId'], keep='last')

        logger.info("Candidates transformed", final_count=len(df))
        return df

    def transform_positions(self, raw_positions: List[Dict]) -> pd.DataFrame:
        """
        Transform multiple position/job order records into a DataFrame.

        Args:
            raw_positions: List of raw position records

        Returns:
            DataFrame with transformed position data
        """
        logger.info("Transforming positions", count=len(raw_positions))

        if not raw_positions:
            logger.warning("No positions to transform")
            return pd.DataFrame()

        transformed = [self.transform_position(p) for p in raw_positions]
        df = pd.DataFrame(transformed)

        # Data quality transformations
        df['title'] = df['title'].str.strip()
        if 'location' in df.columns:
            df['location'] = df['location'].fillna('').str.strip()

        # Remove duplicates based on bullhornId
        df = df.drop_duplicates(subset=['bullhornId'], keep='last')

        logger.info("Positions transformed", final_count=len(df))
        return df

    @staticmethod
    def _safe_join_list(value, default=''):
        """
        Safely join a list that might contain strings or dicts.

        Args:
            value: List of strings or dicts, or None
            default: Default value if list is empty or None

        Returns:
            Joined string
        """
        if not value or not isinstance(value, list):
            return default

        result = []
        for item in value:
            if isinstance(item, str):
                result.append(item)
            elif isinstance(item, dict):
                # Try common keys for dict values
                if 'name' in item:
                    result.append(str(item['name']))
                elif 'title' in item:
                    result.append(str(item['title']))
                elif 'value' in item:
                    result.append(str(item['value']))
                else:
                    # Join all dict values
                    result.append(' '.join(str(v) for v in item.values() if v))
            else:
                result.append(str(item))

        return ', '.join(result) if result else default

    def enrich_with_profile_text(self, df: pd.DataFrame, entity_type: str) -> pd.DataFrame:
        """
        Create a profile text field for ML embedding generation.

        Args:
            df: DataFrame with consultant or candidate data
            entity_type: 'consultant' or 'candidate'

        Returns:
            DataFrame with added 'profileText' column
        """
        logger.info("Enriching with profile text", entity_type=entity_type, count=len(df))

        if entity_type == 'consultant':
            df['profileText'] = df.apply(
                lambda row: f"{row['firstName']} {row['lastName']}. "
                           f"Skills: {self._safe_join_list(row['skills'], 'Not specified')}. "
                           f"Experience: {row['experience'] or 'Not specified'}. "
                           f"Certifications: {self._safe_join_list(row['certifications'], 'None')}. "
                           f"Location: {row['location'] or 'Not specified'}.",
                axis=1
            )
        else:  # candidate
            df['profileText'] = df.apply(
                lambda row: f"{row['firstName']} {row['lastName']}. "
                           f"Skills: {self._safe_join_list(row['skills'], 'Not specified')}. "
                           f"Experience: {row['experience'] or 'Not specified'}. "
                           f"Education: {self._safe_join_list(row['education'], 'Not specified')}. "
                           f"Certifications: {self._safe_join_list(row['certifications'], 'None')}.",
                axis=1
            )

        logger.info("Profile text enrichment complete")
        return df
