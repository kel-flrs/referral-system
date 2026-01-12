# Airflow Pipelines

Apache Airflow orchestration for data pipelines and batch jobs.

## Purpose

Orchestrates ETL workflows and scheduled jobs:
- **Bullhorn sync** - Sync candidates and positions from Bullhorn every 6 hours
- **Embedding generation** - Generate embeddings for new records
- **Matching jobs** - Run ML-based matching algorithm
- **Data quality checks** - Validate and clean data

## Tech Stack

- **Orchestration**: Apache Airflow 2.x
- **Scheduler**: CeleryExecutor (production) / LocalExecutor (dev)
- **Backend**: PostgreSQL
- **Language**: Python 3.11+

## Architecture

```
pipelines/airflow/
├── dags/                    # DAG definitions
│   └── bullhorn_sync_dag.py # Main ETL pipeline
├── plugins/                 # Custom Airflow plugins
├── config/                  # Airflow configuration
└── logs/                    # Execution logs
```

## Main DAG: Bullhorn Sync

**Schedule**: Every 6 hours
**Catchup**: Disabled

### Workflow

```
sync_candidates
    ↓
generate_candidate_embeddings
    ↓
sync_positions
    ↓
generate_position_embeddings
    ↓
find_matches
    ↓
notify (optional)
```

### Tasks

1. **sync_candidates** - Fetch candidates from Bullhorn API
2. **generate_candidate_embeddings** - Call ML service to generate embeddings
3. **sync_positions** - Fetch job orders from Bullhorn API
4. **generate_position_embeddings** - Generate embeddings for positions
5. **find_matches** - Call ML service matching endpoint
6. **notify** - Send notifications (future)

## Development

### Prerequisites

- Python 3.11+
- PostgreSQL (for Airflow metadata)
- Docker & Docker Compose (recommended)

### Setup

```bash
cd pipelines/airflow

# Initialize Airflow database
airflow db init

# Create admin user
airflow users create \
    --username admin \
    --firstname Admin \
    --lastname User \
    --role Admin \
    --email admin@example.com \
    --password admin
```

### Running Locally

#### Option 1: Docker Compose (Recommended)

```bash
# From repository root
npm run infra:up

# Access Airflow UI
open http://localhost:8081
# Login: admin / admin
```

#### Option 2: Local Airflow

```bash
cd pipelines/airflow

# Start webserver
airflow webserver --port 8080

# Start scheduler (in another terminal)
airflow scheduler
```

### Environment Variables

```env
# Airflow
AIRFLOW__CORE__EXECUTOR=LocalExecutor
AIRFLOW__CORE__SQL_ALCHEMY_CONN=postgresql://referral_user:referral_pass@localhost:5432/referral_system
AIRFLOW__CORE__LOAD_EXAMPLES=False

# Database
DB_HOST=localhost
DB_PORT=5432
DB_NAME=referral_system
DB_USER=referral_user
DB_PASSWORD=referral_pass

# Bullhorn
BULLHORN_CLIENT_ID=your_client_id
BULLHORN_CLIENT_SECRET=your_client_secret
BULLHORN_USERNAME=your_username
BULLHORN_PASSWORD=your_password
BULLHORN_REST_URL=https://rest.bullhornstaffing.com/rest-services/

# ML Service
ML_SERVICE_URL=http://ml-service:8000

# Matching
MATCHING_SCORE_THRESHOLD=70
```

## DAG Configuration

### Bullhorn Sync DAG

```python
from airflow import DAG
from datetime import datetime, timedelta

default_args = {
    'owner': 'referral-system',
    'depends_on_past': False,
    'start_date': datetime(2024, 1, 1),
    'email_on_failure': True,
    'email_on_retry': False,
    'retries': 3,
    'retry_delay': timedelta(minutes=5),
}

dag = DAG(
    'bullhorn_sync_optimized',
    default_args=default_args,
    description='Sync Bullhorn data and run matching',
    schedule_interval='0 */6 * * *',  # Every 6 hours
    catchup=False,
    tags=['etl', 'bullhorn', 'production'],
)
```

### Task Dependencies

```python
(
    sync_candidates
    >> generate_candidate_embeddings
    >> sync_positions
    >> generate_position_embeddings
    >> find_matches
)
```

## Monitoring

### Airflow UI

Access at `http://localhost:8081`:
- **DAGs View** - See all DAGs and their schedules
- **Graph View** - Visualize task dependencies
- **Gantt Chart** - View task execution timeline
- **Task Logs** - Debug failed tasks
- **Variables** - Manage configuration
- **Connections** - Manage external connections

### Logging

Logs are stored in `pipelines/airflow/logs/`:
```
logs/
├── dag_id=bullhorn_sync_optimized/
│   ├── run_id=scheduled__2024-01-12T00:00:00+00:00/
│   │   ├── task_id=sync_candidates/
│   │   │   └── attempt=1.log
│   │   └── task_id=find_matches/
│   │       └── attempt=1.log
```

### Metrics

Key metrics to monitor:
- DAG execution time
- Task failure rate
- Retry count
- Data volume processed

## Common Operations

### Trigger DAG Manually

```bash
# Via CLI
airflow dags trigger bullhorn_sync_optimized

# Via UI
# Navigate to DAG → Click "Trigger DAG" button
```

### Clear Failed Tasks

```bash
# Clear specific task
airflow tasks clear bullhorn_sync_optimized sync_candidates -s 2024-01-12

# Clear entire DAG run
airflow dags clear bullhorn_sync_optimized -s 2024-01-12
```

### Pause/Unpause DAG

```bash
# Pause
airflow dags pause bullhorn_sync_optimized

# Unpause
airflow dags unpause bullhorn_sync_optimized
```

### View Task Logs

```bash
airflow tasks logs bullhorn_sync_optimized sync_candidates 2024-01-12
```

## Troubleshooting

### Task Stuck in "Running"

```bash
# Check if task is actually running
airflow tasks state bullhorn_sync_optimized sync_candidates 2024-01-12

# Kill zombie tasks
airflow tasks kill bullhorn_sync_optimized sync_candidates 2024-01-12
```

### DAG Not Appearing

1. Check DAG file syntax: `python dags/bullhorn_sync_dag.py`
2. Check DAG folder path in `airflow.cfg`
3. Restart scheduler: `docker-compose restart airflow-scheduler`

### Connection Timeout

Check service connectivity:
```bash
# Test ML service
curl http://ml-service:8000/health

# Test database
psql -h postgres -U referral_user -d referral_system
```

## Performance Optimization

### Parallelism

```python
# In DAG config
dag = DAG(
    'bullhorn_sync_optimized',
    max_active_runs=1,  # Prevent concurrent DAG runs
    concurrency=16,     # Max concurrent tasks per DAG run
)
```

### Task Timeouts

```python
task = PythonOperator(
    task_id='sync_candidates',
    execution_timeout=timedelta(minutes=30),  # Kill if runs too long
    retries=3,
    retry_delay=timedelta(minutes=5),
)
```

## Deployment

### Production Considerations

1. **Use CeleryExecutor** for distributed task execution
2. **Enable task queues** for prioritization
3. **Set up monitoring** (Prometheus + Grafana)
4. **Configure email alerts** for failures
5. **Use secrets backend** (AWS Secrets Manager, Vault)
6. **Enable task retries** with exponential backoff

### Docker Deployment

Deployed via docker-compose:

```yaml
airflow-webserver:
  build: ./pipelines
  command: webserver
  ports:
    - "8081:8080"

airflow-scheduler:
  build: ./pipelines
  command: scheduler
```

## Testing

### Test DAG Validity

```bash
# Check DAG syntax
python dags/bullhorn_sync_dag.py

# Test import
airflow dags list

# Test specific task
airflow tasks test bullhorn_sync_optimized sync_candidates 2024-01-12
```

### Dry Run

```bash
# Execute task without recording state
airflow tasks run bullhorn_sync_optimized sync_candidates 2024-01-12 --dry-run
```

## Related Components

- **ETL Service**: `services/etl-service` - Called by DAG tasks
- **ML Service**: `services/ml-service` - Called for embeddings and matching
- **Database**: PostgreSQL - Both Airflow metadata and application data
