"""Database connection utilities for data pipeline."""

import os
from contextlib import contextmanager
from typing import Generator

import psycopg2
from psycopg2.extras import RealDictCursor
from sqlalchemy import create_engine
from sqlalchemy.engine import Engine


class DatabaseConnection:
    """Manages database connections for data pipeline."""

    def __init__(self):
        self.host = os.getenv('DB_HOST', 'localhost')
        self.port = os.getenv('DB_PORT', '5433')
        self.database = os.getenv('DB_NAME', 'referral_system')
        self.user = os.getenv('DB_USER', 'referral_user')
        self.password = os.getenv('DB_PASSWORD', 'referral_pass')

    def get_connection_string(self) -> str:
        """Get SQLAlchemy connection string."""
        return f"postgresql://{self.user}:{self.password}@{self.host}:{self.port}/{self.database}"

    def get_engine(self) -> Engine:
        """Get SQLAlchemy engine."""
        return create_engine(
            self.get_connection_string(),
            pool_pre_ping=True,
            pool_size=10,
            max_overflow=20
        )

    @contextmanager
    def get_cursor(self) -> Generator:
        """Get a database cursor with automatic connection management."""
        conn = psycopg2.connect(
            host=self.host,
            port=self.port,
            database=self.database,
            user=self.user,
            password=self.password,
            cursor_factory=RealDictCursor
        )
        cursor = conn.cursor()
        try:
            yield cursor
            conn.commit()
        except Exception as e:
            conn.rollback()
            raise e
        finally:
            cursor.close()
            conn.close()


# Singleton instance
db = DatabaseConnection()
