#!/bin/bash

# =====================================================
# Bullhorn Mock Service - Data Cleanup Script Wrapper
# =====================================================
# This script executes the SQL cleanup script to delete
# all data except oauth_clients, rest_sessions, and
# refresh_tokens tables.
# =====================================================

set -e  # Exit on error

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Default database configuration (from docker-compose.yml)
CONTAINER_NAME="${CONTAINER_NAME:-bullhorn-postgres}"
DB_NAME="${DB_NAME:-bullhorn_mock_dev}"
DB_USER="${DB_USER:-postgres}"

echo -e "${YELLOW}=========================================${NC}"
echo -e "${YELLOW}Bullhorn Mock Service - Data Cleanup${NC}"
echo -e "${YELLOW}=========================================${NC}"
echo ""
echo "This will DELETE all data from tables except:"
echo "  - oauth_clients"
echo "  - rest_sessions"
echo "  - refresh_tokens"
echo ""
echo "Database: ${DB_NAME}"
echo "Container: ${CONTAINER_NAME}"
echo ""

# Check if Docker container is running
if ! docker ps --format '{{.Names}}' | grep -q "^${CONTAINER_NAME}$"; then
    echo -e "${RED}Error: Docker container '${CONTAINER_NAME}' is not running!${NC}"
    echo "Please start it with: docker-compose up -d"
    exit 1
fi

# Confirmation prompt
read -p "Are you sure you want to proceed? (yes/no): " confirmation
if [ "$confirmation" != "yes" ]; then
    echo -e "${RED}Operation cancelled.${NC}"
    exit 0
fi

echo ""
echo -e "${GREEN}Executing cleanup script...${NC}"
echo ""

# Execute the SQL script using docker exec
docker exec -i "$CONTAINER_NAME" psql -U "$DB_USER" -d "$DB_NAME" < cleanup-data.sql

# Check if successful
if [ $? -eq 0 ]; then
    echo ""
    echo -e "${GREEN}=========================================${NC}"
    echo -e "${GREEN}Data cleanup completed successfully!${NC}"
    echo -e "${GREEN}=========================================${NC}"
else
    echo ""
    echo -e "${RED}=========================================${NC}"
    echo -e "${RED}Error occurred during cleanup!${NC}"
    echo -e "${RED}=========================================${NC}"
    exit 1
fi
