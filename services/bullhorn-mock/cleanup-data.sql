-- =====================================================
-- Bullhorn Mock Service - Data Cleanup Script
-- =====================================================
-- This script deletes all data from tables EXCEPT:
--   - oauth_clients
--   - rest_sessions
--   - refresh_tokens
-- =====================================================

BEGIN;

-- Display what will be preserved
\echo '========================================='
\echo 'PRESERVING the following tables:'
\echo '  - oauth_clients'
\echo '  - rest_sessions'
\echo '  - refresh_tokens'
\echo '========================================='
\echo ''
\echo 'DELETING data from:'

-- Delete job-related data (respecting foreign key constraints)
\echo '  - job_submissions'
TRUNCATE TABLE job_submissions CASCADE;

\echo '  - job_order_required_skills'
\echo '  - job_order_preferred_skills'
\echo '  - job_orders'
TRUNCATE TABLE job_orders CASCADE;

-- Delete candidate-related data (respecting foreign key constraints)
\echo '  - candidate_skills'
\echo '  - experiences'
\echo '  - education'
\echo '  - candidates'
TRUNCATE TABLE candidates CASCADE;

-- Delete other business data
\echo '  - activities'
TRUNCATE TABLE activities CASCADE;

\echo '  - consultants'
TRUNCATE TABLE consultants CASCADE;

\echo ''
\echo '========================================='
\echo 'Data cleanup completed successfully!'
\echo '========================================='
\echo ''
\echo 'Preserved tables row counts:'
SELECT 'oauth_clients: ' || COUNT(*) FROM oauth_clients;
SELECT 'rest_sessions: ' || COUNT(*) FROM rest_sessions;
SELECT 'refresh_tokens: ' || COUNT(*) FROM refresh_tokens;

COMMIT;
