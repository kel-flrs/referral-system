-- Create airflow database if it doesn't exist
SELECT 'CREATE DATABASE airflow'
WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'airflow')\gexec

-- Grant privileges to referral_user
GRANT ALL PRIVILEGES ON DATABASE airflow TO referral_user;
