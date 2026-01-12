#!/bin/bash

# Wait for postgres to be ready
echo "Waiting for postgres to be ready..."

max_attempts=30
attempt=0

while [ $attempt -lt $max_attempts ]; do
  if docker exec referral-system-db pg_isready -U referral_user > /dev/null 2>&1; then
    echo "Postgres is ready!"
    exit 0
  fi

  attempt=$((attempt + 1))
  echo "Attempt $attempt/$max_attempts: Postgres not ready yet..."
  sleep 2
done

echo "Error: Postgres did not become ready in time"
exit 1
