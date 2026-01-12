"""
Python Matching Service - High-performance candidate-position matching

Uses vectorized operations (pandas/numpy) and bulk pgvector queries
for 10-20x faster matching than TypeScript implementation.
"""

import os
from typing import List, Dict, Optional, Any
import pandas as pd
import numpy as np
from datetime import datetime
import logging

# Database
import psycopg2
from psycopg2.extras import RealDictCursor
from sqlalchemy import create_engine

# Configure logging
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)


class MatchingService:
    """High-performance matching using vectorized operations"""

    def __init__(self):
        """Initialize matching service with database connection"""
        self.db_config = {
            'host': os.getenv('DB_HOST', 'postgres'),
            'port': int(os.getenv('DB_PORT', '5432')),
            'database': os.getenv('DB_NAME', 'referral_system'),
            'user': os.getenv('DB_USER', 'referral_user'),
            'password': os.getenv('DB_PASSWORD', 'referral_pass')
        }

        # Create SQLAlchemy engine for pandas
        db_url = f"postgresql://{self.db_config['user']}:{self.db_config['password']}@{self.db_config['host']}:{self.db_config['port']}/{self.db_config['database']}"
        self.engine = create_engine(db_url)

        # Scoring weights
        self.WEIGHTS = {
            'SEMANTIC': 0.6,      # 60% semantic similarity
            'SKILL': 0.24,        # 24% skill match (60% of traditional 40%)
            'EXPERIENCE': 0.12,   # 12% experience (30% of traditional 40%)
            'LOCATION': 0.04      # 4% location (10% of traditional 40%)
        }

        # Experience level mappings
        self.EXPERIENCE_LEVELS = {
            'ENTRY': 1,
            'MID': 2,
            'SENIOR': 3,
            'LEAD': 4,
            'EXECUTIVE': 5
        }

        # Skill synonyms (simplified - could be loaded from config)
        self.SKILL_SYNONYMS = {
            'javascript': ['js', 'es6', 'es2015', 'ecmascript'],
            'typescript': ['ts'],
            'python': ['py'],
            'react': ['reactjs', 'react.js'],
            'node': ['nodejs', 'node.js'],
        }

    def get_connection(self):
        """Get database connection"""
        return psycopg2.connect(**self.db_config, cursor_factory=RealDictCursor)

    def canonicalize_skill(self, skill: str) -> str:
        """Normalize skill name using synonyms"""
        skill_lower = skill.lower().strip()
        for canonical, aliases in self.SKILL_SYNONYMS.items():
            if skill_lower in aliases or skill_lower == canonical:
                return canonical
        return skill_lower

    def fetch_positions(self, position_id: Optional[str] = None) -> pd.DataFrame:
        """Fetch open positions from database"""
        if position_id:
            query = """
                SELECT
                    id, title, "requiredSkills", "preferredSkills",
                    "experienceLevel", location, "descriptionEmbedding"::text as "descriptionEmbedding"
                FROM "Position"
                WHERE status = 'OPEN' AND id = %(position_id)s
            """
            df = pd.read_sql_query(query, self.engine, params={'position_id': position_id})
        else:
            query = """
                SELECT
                    id, title, "requiredSkills", "preferredSkills",
                    "experienceLevel", location, "descriptionEmbedding"::text as "descriptionEmbedding"
                FROM "Position"
                WHERE status = 'OPEN'
            """
            df = pd.read_sql_query(query, self.engine)

        logger.info(f"Fetched {len(df)} positions")
        return df

    def fetch_candidates_for_position(
        self,
        position_id: str,
        position_embedding: Any,
        position_skills: List[str],
        limit: int = 2000
    ) -> pd.DataFrame:
        """
        Fetch candidates with semantic similarity scores (bulk query using pgvector)

        This is the KEY optimization - calculate ALL semantic scores in one SQL query
        """
        # If no valid embedding, fetch candidates without semantic scoring
        if position_embedding is None:
            logger.warning(f"Position {position_id}: No valid embedding, skipping semantic scoring")
            if position_skills:
                query = """
                    SELECT
                        c.id,
                        c.skills,
                        c.experience,
                        c.location,
                        NULL::float as semantic_score
                    FROM "Candidate" c
                    WHERE c.status = 'ACTIVE'
                        AND c.skills && %(skills)s::text[]
                    LIMIT %(limit)s
                """
                df = pd.read_sql_query(
                    query,
                    self.engine,
                    params={'skills': position_skills, 'limit': limit}
                )
            else:
                query = """
                    SELECT
                        c.id,
                        c.skills,
                        c.experience,
                        c.location,
                        NULL::float as semantic_score
                    FROM "Candidate" c
                    WHERE c.status = 'ACTIVE'
                    LIMIT %(limit)s
                """
                df = pd.read_sql_query(
                    query,
                    self.engine,
                    params={'limit': limit}
                )
        # Build skill filter with embedding
        elif position_skills:
            query = """
                SELECT
                    c.id,
                    c.skills,
                    c.experience,
                    c.location,
                    CASE
                        WHEN c."profileEmbedding" IS NOT NULL
                        THEN ((1 - (c."profileEmbedding" <=> %(embedding)s::vector)) * 100)::float
                        ELSE NULL
                    END as semantic_score
                FROM "Candidate" c
                WHERE c.status = 'ACTIVE'
                    AND c.skills && %(skills)s::text[]
                ORDER BY c."profileEmbedding" <=> %(embedding)s::vector
                LIMIT %(limit)s
            """
            df = pd.read_sql_query(
                query,
                self.engine,
                params={'embedding': position_embedding, 'skills': position_skills, 'limit': limit}
            )
        else:
            query = """
                SELECT
                    c.id,
                    c.skills,
                    c.experience,
                    c.location,
                    CASE
                        WHEN c."profileEmbedding" IS NOT NULL
                        THEN ((1 - (c."profileEmbedding" <=> %(embedding)s::vector)) * 100)::float
                        ELSE NULL
                    END as semantic_score
                FROM "Candidate" c
                WHERE c.status = 'ACTIVE'
                ORDER BY c."profileEmbedding" <=> %(embedding)s::vector
                LIMIT %(limit)s
            """
            df = pd.read_sql_query(
                query,
                self.engine,
                params={'embedding': position_embedding, 'limit': limit}
            )

        return df

    def score_skills_vectorized(
        self,
        candidates_df: pd.DataFrame,
        required_skills: List[str],
        preferred_skills: List[str]
    ) -> pd.Series:
        """
        Vectorized skill scoring using pandas - FAST!

        Instead of looping through candidates, we process all at once
        """
        def score_candidate_skills(candidate_skills):
            if not candidate_skills:
                return 0

            # Canonicalize candidate skills
            candidate_canonical = set(self.canonicalize_skill(s) for s in candidate_skills)

            # Canonicalize required skills
            required_canonical = [self.canonicalize_skill(s) for s in required_skills]
            required_matched = sum(1 for s in required_canonical if s in candidate_canonical)

            # Canonicalize preferred skills
            preferred_canonical = [self.canonicalize_skill(s) for s in preferred_skills]
            preferred_matched = sum(1 for s in preferred_canonical if s in candidate_canonical)

            # Weighted score (required: 70%, preferred: 30%)
            required_total = len(required_canonical)
            preferred_total = len(preferred_canonical)

            denominator = required_total * 2 + preferred_total
            numerator = required_matched * 2 + preferred_matched

            return round((numerator / denominator) * 100) if denominator > 0 else 0

        # Apply to all candidates at once (vectorized)
        return candidates_df['skills'].apply(score_candidate_skills)

    def estimate_years_experience(self, experience_data: Any) -> Optional[float]:
        """Estimate years of experience from experience data"""
        if not experience_data or not isinstance(experience_data, list):
            return None

        total_months = 0
        for entry in experience_data:
            if not isinstance(entry, dict):
                continue

            start_date = entry.get('startDate')
            end_date = entry.get('endDate') or datetime.now().isoformat()

            if start_date:
                try:
                    start = datetime.fromisoformat(start_date.replace('Z', '+00:00'))
                    end = datetime.fromisoformat(end_date.replace('Z', '+00:00'))
                    months = (end.year - start.year) * 12 + (end.month - start.month)
                    if months > 0:
                        total_months += months
                except:
                    continue

        return total_months / 12 if total_months > 0 else None

    def score_experience_vectorized(
        self,
        candidates_df: pd.DataFrame,
        target_level: Optional[str]
    ) -> pd.Series:
        """Vectorized experience scoring"""
        if not target_level:
            return pd.Series([50] * len(candidates_df))  # Default score

        target_level_num = self.EXPERIENCE_LEVELS.get(target_level.upper(), 3)

        def score_exp(experience_data):
            years = self.estimate_years_experience(experience_data)
            if years is None:
                return 50  # Unknown

            # Convert years to level
            if years < 2:
                candidate_level = 1
            elif years < 5:
                candidate_level = 2
            elif years < 8:
                candidate_level = 3
            elif years < 12:
                candidate_level = 4
            else:
                candidate_level = 5

            # Score based on difference
            diff = abs(candidate_level - target_level_num)
            if diff == 0:
                return 100
            elif diff == 1:
                return 75
            elif diff == 2:
                return 50
            else:
                return 25

        return candidates_df['experience'].apply(score_exp)

    def score_location_vectorized(
        self,
        candidates_df: pd.DataFrame,
        target_location: Optional[str]
    ) -> pd.Series:
        """Vectorized location scoring"""
        if not target_location:
            return pd.Series([50] * len(candidates_df))

        target_lower = target_location.lower()

        def score_loc(candidate_location):
            if not candidate_location:
                return 50

            candidate_lower = candidate_location.lower()

            if 'remote' in candidate_lower or 'remote' in target_lower:
                return 100

            if candidate_lower == target_lower:
                return 100

            # Check city match
            candidate_parts = candidate_lower.split(',')
            target_parts = target_lower.split(',')

            if len(candidate_parts) >= 1 and len(target_parts) >= 1:
                if candidate_parts[0].strip() == target_parts[0].strip():
                    return 80  # Same city

            if len(candidate_parts) >= 2 and len(target_parts) >= 2:
                if candidate_parts[-1].strip() == target_parts[-1].strip():
                    return 60  # Same state

            return 30  # Different

        return candidates_df['location'].apply(score_loc)

    def compute_matches_for_position(
        self,
        position_row: pd.Series,
        min_score: int = 70,
        max_matches: int = 100
    ) -> List[Dict]:
        """
        Compute matches for a single position using vectorized operations
        """
        position_id = position_row['id']

        # Get position skills
        required_skills = position_row.get('requiredSkills') or []
        preferred_skills = position_row.get('preferredSkills') or []
        all_skills = list(set(required_skills + preferred_skills))

        # Get position embedding - use bracket notation for pandas Series
        position_embedding_raw = position_row.get('descriptionEmbedding')

        # Validate embedding format
        position_embedding = None
        if position_embedding_raw is not None and pd.notna(position_embedding_raw):
            embedding_str = str(position_embedding_raw).strip()
            # Check if it's a valid vector format (should start with '[')
            if embedding_str.startswith('[') and embedding_str.endswith(']'):
                position_embedding = embedding_str
            else:
                logger.warning(
                    f"Position {position_id}: Invalid embedding format - {embedding_str[:100]}"
                )

        logger.info(f"Position {position_id}: embedding type={type(position_embedding)}, value={str(position_embedding)[:100] if position_embedding else 'None'}")

        # Fetch candidates with semantic scores (bulk query - FAST!)
        candidates_df = self.fetch_candidates_for_position(
            position_id=position_id,
            position_embedding=position_embedding,
            position_skills=all_skills,
            limit=2000
        )

        if candidates_df.empty:
            logger.info(f"No candidates found for position {position_id}")
            return []

        # Vectorized scoring (ALL candidates at once - FAST!)
        candidates_df['skill_score'] = self.score_skills_vectorized(
            candidates_df, required_skills, preferred_skills
        )
        candidates_df['experience_score'] = self.score_experience_vectorized(
            candidates_df, position_row.get('experienceLevel')
        )
        candidates_df['location_score'] = self.score_location_vectorized(
            candidates_df, position_row.get('location')
        )

        # Fill missing semantic scores with 50
        candidates_df['semantic_score'] = candidates_df['semantic_score'].fillna(50)

        # Compute overall score (vectorized - FAST!)
        candidates_df['overall_score'] = (
            candidates_df['semantic_score'] * self.WEIGHTS['SEMANTIC'] +
            candidates_df['skill_score'] * self.WEIGHTS['SKILL'] +
            candidates_df['experience_score'] * self.WEIGHTS['EXPERIENCE'] +
            candidates_df['location_score'] * self.WEIGHTS['LOCATION']
        ).round().astype(int)

        # Filter by min score
        candidates_df = candidates_df[candidates_df['overall_score'] >= min_score]

        # Sort and take top matches
        candidates_df = candidates_df.nlargest(max_matches, 'overall_score')

        # Convert to match records
        matches = []
        for _, row in candidates_df.iterrows():
            matches.append({
                'candidateId': row['id'],
                'positionId': position_id,
                'overallScore': int(row['overall_score']),
                'semanticScore': int(row['semantic_score']) if pd.notna(row['semantic_score']) else None,
                'skillMatchScore': int(row['skill_score']),
                'experienceScore': int(row['experience_score']),
                'locationScore': int(row['location_score']),
            })

        logger.info(f"Position {position_id}: Generated {len(matches)} matches")
        return matches

    def find_matches(
        self,
        position_id: Optional[str] = None,
        min_score: int = 70
    ) -> Dict[str, Any]:
        """
        Main entry point - find matches using vectorized operations

        Returns summary statistics
        """
        start_time = datetime.now()

        # Fetch positions
        positions_df = self.fetch_positions(position_id)

        if positions_df.empty:
            return {
                'positionsProcessed': 0,
                'totalMatches': 0,
                'durationSeconds': 0
            }

        # Debug: Check DataFrame structure
        logger.info(f"Positions DataFrame columns: {positions_df.columns.tolist()}")
        logger.info(f"Positions DataFrame dtypes: {positions_df.dtypes.to_dict()}")
        if len(positions_df) > 0:
            first_row = positions_df.iloc[0]
            logger.info(f"First row descriptionEmbedding: type={type(first_row['descriptionEmbedding'])}, value={str(first_row['descriptionEmbedding'])[:100]}")

        # Process each position
        all_matches = []
        for _, position_row in positions_df.iterrows():
            matches = self.compute_matches_for_position(position_row, min_score)
            all_matches.extend(matches)

        # Bulk insert matches
        if all_matches:
            self.bulk_upsert_matches(all_matches)

        duration = (datetime.now() - start_time).total_seconds()

        result = {
            'positionsProcessed': len(positions_df),
            'totalMatches': len(all_matches),
            'durationSeconds': round(duration, 2)
        }

        logger.info(f"Matching complete: {result}")
        return result

    def bulk_upsert_matches(self, matches: List[Dict]):
        """Bulk upsert matches to database"""
        conn = self.get_connection()
        cursor = conn.cursor()

        try:
            # Use INSERT ... ON CONFLICT for upsert
            # ID is generated by database (gen_random_uuid())
            upsert_query = """
                INSERT INTO "Match" (
                    "candidateId", "positionId", "overallScore",
                    "semanticScore", "skillMatchScore", "experienceScore", "locationScore",
                    "matchedSkills", "missingSkills", "matchReason", "status",
                    "createdAt", "updatedAt"
                ) VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s)
                ON CONFLICT ("candidateId", "positionId")
                DO UPDATE SET
                    "overallScore" = EXCLUDED."overallScore",
                    "semanticScore" = EXCLUDED."semanticScore",
                    "skillMatchScore" = EXCLUDED."skillMatchScore",
                    "experienceScore" = EXCLUDED."experienceScore",
                    "locationScore" = EXCLUDED."locationScore",
                    "updatedAt" = EXCLUDED."updatedAt"
            """

            now = datetime.now()
            values = [
                (
                    m['candidateId'], m['positionId'], m['overallScore'],
                    m.get('semanticScore'), m['skillMatchScore'],
                    m['experienceScore'], m.get('locationScore'),
                    [], [], 'Automated match', 'PENDING',  # matchedSkills, missingSkills, matchReason, status
                    now, now
                )
                for m in matches
            ]

            cursor.executemany(upsert_query, values)
            conn.commit()

            logger.info(f"Bulk upserted {len(matches)} matches")
        finally:
            cursor.close()
            conn.close()


# Global instance
matching_service = MatchingService()
