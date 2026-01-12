"""
Generate embeddings for candidates and positions using the ML service.

This module handles batch embedding generation and updates the database
with vector embeddings for semantic matching.
"""

import os
import requests
from typing import List, Dict, Optional, Tuple
import time
import psycopg2
from concurrent.futures import ThreadPoolExecutor, as_completed
import threading

from data_platform.utils.logger import get_logger
from data_platform.utils.database import db

logger = get_logger(__name__)


class EmbeddingGenerator:
    """Generates embeddings for candidates and positions with parallel processing."""

    def __init__(
        self,
        ml_service_url: Optional[str] = None,
        batch_size: int = 500,
        max_workers: int = 5
    ):
        """
        Initialize the embedding generator.

        Args:
            ml_service_url: URL of the ML service (defaults to env var)
            batch_size: Number of records to process in each batch (default: 500)
            max_workers: Number of parallel workers for batch processing (default: 5)
        """
        self.ml_service_url = (ml_service_url or os.getenv('ML_SERVICE_URL', 'http://localhost:8000')).rstrip('/')
        self.batch_size = batch_size
        self.max_workers = max_workers
        self.lock = threading.Lock()
        logger.info(
            "Initialized EmbeddingGenerator",
            url=self.ml_service_url,
            batch_size=batch_size,
            max_workers=max_workers
        )

    def _get_db_connection(self):
        """Get a new database connection for thread-safe operations."""
        return psycopg2.connect(
            host=db.host,
            port=db.port,
            database=db.database,
            user=db.user,
            password=db.password
        )

    def generate_embeddings_batch(self, texts: List[str]) -> List[List[float]]:
        """
        Generate embeddings for a batch of texts.

        Args:
            texts: List of text strings to embed

        Returns:
            List of embedding vectors

        Raises:
            Exception: If the ML service call fails
        """
        url = f"{self.ml_service_url}/embeddings/batch"
        payload = {"texts": texts}

        try:
            response = requests.post(url, json=payload, timeout=120)
            response.raise_for_status()
            result = response.json()
            return result['embeddings']
        except Exception as e:
            logger.error("Failed to generate embeddings", error=str(e), text_count=len(texts))
            raise

    def _process_candidate_batch(
        self,
        batch: List[Tuple],
        batch_num: int,
        total_batches: int
    ) -> Dict[str, int]:
        """
        Process a single batch of candidates (thread-safe).

        Args:
            batch: List of candidate tuples from database
            batch_num: Batch number for logging
            total_batches: Total number of batches

        Returns:
            Dictionary with processed, updated, errors counts
        """
        batch_ids = [c[0] for c in batch]

        # Create profile texts
        profile_texts = []
        for c in batch:
            candidate_id, first_name, last_name, title, summary, skills = c
            skills_text = ', '.join(skills) if skills else 'Not specified'
            summary_text = summary if summary else 'Not specified'
            title_text = title if title else 'Not specified'

            profile_text = (
                f"{first_name} {last_name}. "
                f"Current Title: {title_text}. "
                f"Summary: {summary_text}. "
                f"Skills: {skills_text}."
            )
            profile_texts.append(profile_text)

        try:
            # Generate embeddings
            embeddings = self.generate_embeddings_batch(profile_texts)

            # Update database (each thread gets its own connection)
            conn = self._get_db_connection()
            cursor = conn.cursor()

            try:
                # Bulk update using executemany
                update_data = [
                    ('[' + ','.join(map(str, embedding)) + ']', candidate_id)
                    for candidate_id, embedding in zip(batch_ids, embeddings)
                ]

                cursor.executemany(
                    'UPDATE "Candidate" SET "profileEmbedding" = %s::vector WHERE id = %s',
                    update_data
                )
                conn.commit()

                updated = len(batch)

                logger.info(
                    "Processed candidate batch",
                    batch_num=batch_num,
                    total_batches=total_batches,
                    records=updated
                )

                return {'processed': updated, 'updated': updated, 'errors': 0}

            finally:
                cursor.close()
                conn.close()

        except Exception as e:
            logger.error("Error processing candidate batch", error=str(e), batch_num=batch_num)
            return {'processed': 0, 'updated': 0, 'errors': len(batch)}

    def _process_position_batch(
        self,
        batch: List[Tuple],
        batch_num: int,
        total_batches: int
    ) -> Dict[str, int]:
        """
        Process a single batch of positions (thread-safe).

        Args:
            batch: List of position tuples from database
            batch_num: Batch number for logging
            total_batches: Total number of batches

        Returns:
            Dictionary with processed, updated, errors counts
        """
        batch_ids = [p[0] for p in batch]

        # Create job description texts
        job_texts = []
        for p in batch:
            position_id, title, description, required_skills, preferred_skills, exp_level, location = p

            required_text = ', '.join(required_skills) if required_skills else 'Not specified'
            preferred_text = ', '.join(preferred_skills) if preferred_skills else 'Not specified'
            description_text = description if description else 'Not specified'
            exp_text = exp_level if exp_level else 'Not specified'
            location_text = location if location else 'Not specified'

            job_text = (
                f"{title}. "
                f"Description: {description_text}. "
                f"Required Skills: {required_text}. "
                f"Preferred Skills: {preferred_text}. "
                f"Experience Level: {exp_text}. "
                f"Location: {location_text}."
            )
            job_texts.append(job_text)

        try:
            # Generate embeddings
            embeddings = self.generate_embeddings_batch(job_texts)

            # Update database (each thread gets its own connection)
            conn = self._get_db_connection()
            cursor = conn.cursor()

            try:
                # Bulk update using executemany
                update_data = [
                    ('[' + ','.join(map(str, embedding)) + ']', position_id)
                    for position_id, embedding in zip(batch_ids, embeddings)
                ]

                cursor.executemany(
                    'UPDATE "Position" SET "descriptionEmbedding" = %s::vector WHERE id = %s',
                    update_data
                )
                conn.commit()

                updated = len(batch)

                logger.info(
                    "Processed position batch",
                    batch_num=batch_num,
                    total_batches=total_batches,
                    records=updated
                )

                return {'processed': updated, 'updated': updated, 'errors': 0}

            finally:
                cursor.close()
                conn.close()

        except Exception as e:
            logger.error("Error processing position batch", error=str(e), batch_num=batch_num)
            return {'processed': 0, 'updated': 0, 'errors': len(batch)}

    def generate_candidate_embeddings(self) -> Dict[str, int]:
        """
        Generate embeddings for all candidates without embeddings using parallel processing.

        Returns:
            Dictionary with statistics (processed, updated, errors)
        """
        logger.info("Starting PARALLEL candidate embedding generation", max_workers=self.max_workers)
        start_time = time.time()

        # Fetch candidates without embeddings
        conn = self._get_db_connection()
        cursor = conn.cursor()

        try:
            cursor.execute("""
                SELECT id, "firstName", "lastName", "currentTitle", summary, skills
                FROM "Candidate"
                WHERE "profileEmbedding" IS NULL
                ORDER BY "createdAt" DESC
            """)

            candidates = cursor.fetchall()
            total_candidates = len(candidates)
            logger.info("Found candidates to process", count=total_candidates)

            if total_candidates == 0:
                return {'processed': 0, 'updated': 0, 'errors': 0}

            # Split into batches
            batches = []
            for i in range(0, total_candidates, self.batch_size):
                batch = candidates[i:i + self.batch_size]
                batches.append((batch, i // self.batch_size + 1))

            total_batches = len(batches)
            logger.info("Processing in parallel", batches=total_batches, workers=self.max_workers)

            # Process batches in parallel
            total_processed = 0
            total_updated = 0
            total_errors = 0
            completed_batches = 0

            with ThreadPoolExecutor(max_workers=self.max_workers) as executor:
                # Submit all batch jobs
                future_to_batch = {
                    executor.submit(self._process_candidate_batch, batch, batch_num, total_batches): batch_num
                    for batch, batch_num in batches
                }

                # Collect results as they complete
                for future in as_completed(future_to_batch):
                    batch_num = future_to_batch[future]
                    try:
                        result = future.result()
                        total_processed += result['processed']
                        total_updated += result['updated']
                        total_errors += result['errors']
                        completed_batches += 1

                        # Log cumulative progress
                        progress_pct = (completed_batches / total_batches) * 100
                        logger.info(
                            "Candidate embedding progress",
                            completed=total_processed,
                            total=total_candidates,
                            percentage=f"{progress_pct:.1f}%",
                            batches_completed=f"{completed_batches}/{total_batches}"
                        )
                    except Exception as e:
                        logger.error("Batch failed", batch_num=batch_num, error=str(e))
                        total_errors += self.batch_size
                        completed_batches += 1

            duration = time.time() - start_time
            records_per_second = total_processed / duration if duration > 0 else 0

            logger.info(
                "PARALLEL candidate embedding generation complete",
                processed=total_processed,
                updated=total_updated,
                errors=total_errors,
                duration_seconds=round(duration, 2),
                records_per_second=round(records_per_second, 2),
                batches=total_batches,
                workers=self.max_workers
            )

            return {
                'processed': total_processed,
                'updated': total_updated,
                'errors': total_errors,
                'duration_seconds': duration
            }

        finally:
            cursor.close()
            conn.close()

    def generate_position_embeddings(self) -> Dict[str, int]:
        """
        Generate embeddings for all positions without embeddings using parallel processing.

        Returns:
            Dictionary with statistics (processed, updated, errors)
        """
        logger.info("Starting PARALLEL position embedding generation", max_workers=self.max_workers)
        start_time = time.time()

        # Fetch positions without embeddings
        conn = self._get_db_connection()
        cursor = conn.cursor()

        try:
            cursor.execute("""
                SELECT id, title, description, "requiredSkills", "preferredSkills", "experienceLevel", location
                FROM "Position"
                WHERE "descriptionEmbedding" IS NULL
                ORDER BY "createdAt" DESC
            """)

            positions = cursor.fetchall()
            total_positions = len(positions)
            logger.info("Found positions to process", count=total_positions)

            if total_positions == 0:
                return {'processed': 0, 'updated': 0, 'errors': 0}

            # Split into batches
            batches = []
            for i in range(0, total_positions, self.batch_size):
                batch = positions[i:i + self.batch_size]
                batches.append((batch, i // self.batch_size + 1))

            total_batches = len(batches)
            logger.info("Processing in parallel", batches=total_batches, workers=self.max_workers)

            # Process batches in parallel
            total_processed = 0
            total_updated = 0
            total_errors = 0
            completed_batches = 0

            with ThreadPoolExecutor(max_workers=self.max_workers) as executor:
                # Submit all batch jobs
                future_to_batch = {
                    executor.submit(self._process_position_batch, batch, batch_num, total_batches): batch_num
                    for batch, batch_num in batches
                }

                # Collect results as they complete
                for future in as_completed(future_to_batch):
                    batch_num = future_to_batch[future]
                    try:
                        result = future.result()
                        total_processed += result['processed']
                        total_updated += result['updated']
                        total_errors += result['errors']
                        completed_batches += 1

                        # Log cumulative progress
                        progress_pct = (completed_batches / total_batches) * 100
                        logger.info(
                            "Position embedding progress",
                            completed=total_processed,
                            total=total_positions,
                            percentage=f"{progress_pct:.1f}%",
                            batches_completed=f"{completed_batches}/{total_batches}"
                        )
                    except Exception as e:
                        logger.error("Batch failed", batch_num=batch_num, error=str(e))
                        total_errors += self.batch_size
                        completed_batches += 1

            duration = time.time() - start_time
            records_per_second = total_processed / duration if duration > 0 else 0

            logger.info(
                "PARALLEL position embedding generation complete",
                processed=total_processed,
                updated=total_updated,
                errors=total_errors,
                duration_seconds=round(duration, 2),
                records_per_second=round(records_per_second, 2),
                batches=total_batches,
                workers=self.max_workers
            )

            return {
                'processed': total_processed,
                'updated': total_updated,
                'errors': total_errors,
                'duration_seconds': duration
            }

        finally:
            cursor.close()
            conn.close()
