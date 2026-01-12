# Data Pipeline Quick Start Guide

Get your Python-based data engineering pipeline running in **5 minutes**.

## Prerequisites

- Docker and Docker Compose installed
- Bullhorn Mock Service running on `http://localhost:8080`
- PostgreSQL configured (included in docker-compose)

## Step 1: Configure Environment

Update `airflow/.env.airflow` with your Bullhorn credentials:

```bash
BULLHORN_MOCK_BASE_URL=http://host.docker.internal:8080
BULLHORN_MOCK_CLIENT_ID=your-client-id
BULLHORN_MOCK_USERNAME=admin
BULLHORN_MOCK_PASSWORD=admin
```

## Step 2: Start All Services

```bash
# Build and start all containers
docker-compose up -d

# Wait for services to be healthy (30-60 seconds)
docker-compose ps
```

You should see:
- `referral-system-db` (PostgreSQL)
- `referral-system-redis` (Redis)
- `referral-system-ml` (ML Service)
- `referral-system-airflow-webserver` (Airflow UI)
- `referral-system-airflow-scheduler` (Airflow Scheduler)

## Step 3: Initialize Airflow

```bash
# Initialize Airflow database
docker exec referral-system-airflow-webserver airflow db init

# Create admin user
docker exec referral-system-airflow-webserver airflow users create \
    --username admin \
    --firstname Admin \
    --lastname User \
    --role Admin \
    --email admin@example.com \
    --password admin

# Verify DAGs are loaded
docker exec referral-system-airflow-webserver airflow dags list
```

You should see: `bullhorn_sync`

## Step 4: Access Airflow UI

1. Open browser: http://localhost:8081
2. Login with: `admin` / `admin`
3. You'll see the Airflow dashboard

## Step 5: Run Your First Pipeline

### Manual Trigger (Recommended for First Run)

1. Click on `bullhorn_sync` DAG
2. Click **"Trigger DAG"** button (play icon)
3. Click on the running DAG instance
4. Watch tasks execute in real-time

### Task Execution Order

```
[1] extract_bullhorn_data (2-5 min)
        ↓
[2] transform_consultants + transform_candidates (parallel, 1-2 min)
        ↓
[3] validate_consultants + validate_candidates (parallel, <1 min)
        ↓
[4] load_consultants + load_candidates (parallel, 2-5 min)
        ↓
[5] generate_embeddings (<1 min)
```

### View Logs

Click on any task → Click **"Log"** button to see detailed execution logs.

## Step 6: Verify Data Loaded

```bash
# Connect to PostgreSQL
docker exec -it referral-system-db psql -U referral_user -d referral_system

# Check consultant count
SELECT COUNT(*) FROM "Candidate" WHERE type = 'CONSULTANT';

# Check candidate count
SELECT COUNT(*) FROM "Candidate" WHERE type = 'CANDIDATE';

# View recent pipeline runs
SELECT * FROM pipeline_runs ORDER BY started_at DESC LIMIT 5;

# Exit psql
\q
```

## Step 7: Enable Automatic Scheduling

The DAG is configured to run **every 6 hours** automatically.

To enable:
1. In Airflow UI, toggle the **ON/OFF switch** next to `bullhorn_sync`
2. The pipeline will now run automatically every 6 hours

## Common Commands

### View Logs

```bash
# Airflow webserver logs
docker logs referral-system-airflow-webserver

# Airflow scheduler logs
docker logs referral-system-airflow-scheduler

# Follow logs in real-time
docker logs -f referral-system-airflow-scheduler
```

### Restart Services

```bash
# Restart Airflow
docker-compose restart airflow-webserver airflow-scheduler

# Restart all services
docker-compose restart
```

### Stop Services

```bash
# Stop all services
docker-compose down

# Stop and remove volumes (CAUTION: deletes data)
docker-compose down -v
```

### Test Individual Task

```bash
# Test extract task
docker exec referral-system-airflow-webserver \
    airflow tasks test bullhorn_sync extract_bullhorn_data 2024-01-01

# Test transform task
docker exec referral-system-airflow-webserver \
    airflow tasks test bullhorn_sync transform_consultants 2024-01-01
```

## Monitoring Dashboard

Access Airflow UI at http://localhost:8081

### Key Pages

- **DAGs**: List of all pipelines
- **Grid View**: Visual timeline of DAG runs
- **Graph View**: Task dependencies
- **Task Duration**: Performance metrics
- **Logs**: Detailed task logs

### Understanding Task States

- 🟢 **Success**: Task completed successfully
- **Failed**: Task failed (will retry 3 times)
- 🟡 **Running**: Task currently executing
- ⚪ **Queued**: Task waiting to run
- 🟠 **Upstream Failed**: Previous task failed
- 🔵 **Skipped**: Task was skipped

## Troubleshooting

### DAG Not Showing Up

```bash
# Check for import errors
docker exec referral-system-airflow-webserver airflow dags list-import-errors

# Check DAG syntax
docker exec referral-system-airflow-webserver python /opt/airflow/dags/bullhorn_sync_dag.py
```

### Task Failing with "Connection Refused"

- Ensure Bullhorn Mock Service is running on port 8080
- Check `BULLHORN_MOCK_BASE_URL` in `airflow/.env.airflow`
- Use `host.docker.internal` instead of `localhost` for Mac/Windows

### "No module named 'data_platform'"

```bash
# Rebuild Airflow image
docker-compose build airflow-webserver airflow-scheduler

# Restart services
docker-compose up -d
```

### Database Connection Error

```bash
# Check PostgreSQL is running
docker-compose ps postgres

# Test connection
docker exec referral-system-airflow-webserver python -c "
from data_platform.utils.database import db
with db.get_cursor() as cursor:
    cursor.execute('SELECT 1')
    print('✓ Database connected!')
"
```

## What's Happening Behind the Scenes?

1. **Airflow Scheduler** checks every minute for DAGs to run
2. **DAG** triggers based on schedule (every 6 hours)
3. **Extractor** authenticates with Bullhorn and fetches data
4. **Transformer** cleans and normalizes the data
5. **Validator** runs data quality checks
6. **Loader** upserts data to PostgreSQL (updates existing, inserts new)
7. **Pipeline metadata** logged to `pipeline_runs` table

## Next Steps

Once your first pipeline run succeeds:

1. **Review logs** in Airflow UI to understand execution
2. **Check database** to verify data was loaded
3. **Enable automatic scheduling** (toggle DAG on)
4. **Set up alerts** (configure email in `default_args`)
5. **Monitor regularly** via Airflow UI dashboard

## Performance Benchmarks

Expected performance for different data volumes:

| Records | Extract | Transform | Validate | Load | Total |
|---------|---------|-----------|----------|------|-------|
| 1,000 | 30s | 10s | 5s | 20s | ~1 min |
| 10,000 | 2 min | 30s | 10s | 1 min | ~4 min |
| 100,000 | 15 min | 3 min | 30s | 8 min | ~27 min |
| 500,000 | 1 hour | 12 min | 2 min | 35 min | ~2 hours |

## Support

**Full Documentation**: See `DATA_ENGINEERING_README.md`

**Airflow Docs**: https://airflow.apache.org/docs/

**Quick Help**:
```bash
# Check all services status
docker-compose ps

# View all logs
docker-compose logs

# Restart everything
docker-compose restart
```

---

**You're now running a production-grade data engineering pipeline!** 
