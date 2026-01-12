#!/usr/bin/env python3
"""
Script to check and report on embedding data quality in the database.

This script identifies positions and candidates with invalid or missing embeddings.
"""

import os
import sys
import psycopg2
from psycopg2.extras import RealDictCursor

def get_db_connection():
    """Get database connection."""
    return psycopg2.connect(
        host=os.getenv('DB_HOST', 'localhost'),
        port=int(os.getenv('DB_PORT', '5432')),
        database=os.getenv('DB_NAME', 'referral_system'),
        user=os.getenv('DB_USER', 'referral_user'),
        password=os.getenv('DB_PASSWORD', 'referral_pass'),
        cursor_factory=RealDictCursor
    )

def check_position_embeddings():
    """Check position embeddings for issues."""
    conn = get_db_connection()
    cursor = conn.cursor()

    try:
        # Count total open positions
        cursor.execute('SELECT COUNT(*) as total FROM "Position" WHERE status = \'OPEN\'')
        total = cursor.fetchone()['total']

        # Count positions with NULL embeddings
        cursor.execute('SELECT COUNT(*) as count FROM "Position" WHERE status = \'OPEN\' AND "descriptionEmbedding" IS NULL')
        null_count = cursor.fetchone()['count']

        # Check for positions with potentially invalid embeddings
        # (This is harder to detect, but we can try to cast and catch errors)
        cursor.execute('''
            SELECT id, title, "descriptionEmbedding"::text as embedding_text
            FROM "Position"
            WHERE status = 'OPEN' AND "descriptionEmbedding" IS NOT NULL
            LIMIT 5
        ''')
        sample_positions = cursor.fetchall()

        print("\n=== Position Embeddings Report ===")
        print(f"Total open positions: {total}")
        print(f"Positions with NULL embeddings: {null_count} ({null_count/total*100:.1f}%)")
        print(f"Positions with embeddings: {total - null_count}")

        if sample_positions:
            print("\nSample embeddings:")
            for pos in sample_positions:
                embedding_preview = pos['embedding_text'][:50] if pos['embedding_text'] else 'NULL'
                print(f"  - Position {pos['id']} ({pos['title']}): {embedding_preview}...")

                # Check if it's a valid vector format
                if pos['embedding_text']:
                    if not pos['embedding_text'].strip().startswith('['):
                        print(f"    ⚠️  WARNING: Invalid embedding format!")

    finally:
        cursor.close()
        conn.close()

def check_candidate_embeddings():
    """Check candidate embeddings for issues."""
    conn = get_db_connection()
    cursor = conn.cursor()

    try:
        # Count total active candidates
        cursor.execute('SELECT COUNT(*) as total FROM "Candidate" WHERE status = \'ACTIVE\'')
        total = cursor.fetchone()['total']

        # Count candidates with NULL embeddings
        cursor.execute('SELECT COUNT(*) as count FROM "Candidate" WHERE status = \'ACTIVE\' AND "profileEmbedding" IS NULL')
        null_count = cursor.fetchone()['count']

        # Check for candidates with potentially invalid embeddings
        cursor.execute('''
            SELECT id, "firstName", "lastName", "profileEmbedding"::text as embedding_text
            FROM "Candidate"
            WHERE status = 'ACTIVE' AND "profileEmbedding" IS NOT NULL
            LIMIT 5
        ''')
        sample_candidates = cursor.fetchall()

        print("\n=== Candidate Embeddings Report ===")
        print(f"Total active candidates: {total}")
        print(f"Candidates with NULL embeddings: {null_count} ({null_count/total*100:.1f}%)")
        print(f"Candidates with embeddings: {total - null_count}")

        if sample_candidates:
            print("\nSample embeddings:")
            for cand in sample_candidates:
                embedding_preview = cand['embedding_text'][:50] if cand['embedding_text'] else 'NULL'
                print(f"  - Candidate {cand['id']} ({cand['firstName']} {cand['lastName']}): {embedding_preview}...")

                # Check if it's a valid vector format
                if cand['embedding_text']:
                    if not cand['embedding_text'].strip().startswith('['):
                        print(f"    ⚠️  WARNING: Invalid embedding format!")

    finally:
        cursor.close()
        conn.close()

def clean_invalid_embeddings():
    """Set invalid embeddings to NULL so they can be regenerated."""
    conn = get_db_connection()
    cursor = conn.cursor()

    try:
        print("\n=== Cleaning Invalid Embeddings ===")

        # This is a simplified check - in reality, we'd need to validate the vector format more thoroughly
        # For now, we'll just identify NULLs

        cursor.execute('''
            SELECT id FROM "Position"
            WHERE status = 'OPEN' AND "descriptionEmbedding" IS NULL
        ''')
        positions_to_fix = cursor.fetchall()

        cursor.execute('''
            SELECT id FROM "Candidate"
            WHERE status = 'ACTIVE' AND "profileEmbedding" IS NULL
        ''')
        candidates_to_fix = cursor.fetchall()

        print(f"Found {len(positions_to_fix)} positions needing embeddings")
        print(f"Found {len(candidates_to_fix)} candidates needing embeddings")
        print("\nTo generate embeddings, run the embedding generation job.")

    finally:
        cursor.close()
        conn.close()

if __name__ == '__main__':
    try:
        print("Checking embedding data quality...\n")
        check_position_embeddings()
        check_candidate_embeddings()
        clean_invalid_embeddings()
        print("\n✓ Check complete!")
    except Exception as e:
        print(f"\n✗ Error: {e}", file=sys.stderr)
        sys.exit(1)
