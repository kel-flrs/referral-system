#!/bin/bash
set -e

echo "Initializing Airflow database..."
# Initialize/upgrade Airflow database (idempotent)
airflow db migrate

echo "Setting up admin user..."
# Create admin user only if it doesn't exist
airflow users create \
    --username admin \
    --firstname Admin \
    --lastname User \
    --role Admin \
    --email admin@example.com \
    --password admin 2>/dev/null || echo "Admin user already exists, skipping creation"

echo "Airflow initialization complete!"
