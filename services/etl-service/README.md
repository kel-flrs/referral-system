# ETL Service

Extract, Transform, Load service for syncing data from various CRM systems.

## Purpose

Handles data synchronization from multiple CRM platforms:
- **Bullhorn** - Primary recruiting CRM
- **HubSpot** - Marketing and sales CRM (future)
- **Salesforce** - Enterprise CRM (future)

Includes data validation, transformation, and embedding generation.

## Tech Stack

- **Language**: Python 3.11+
- **Database**: PostgreSQL
- **Key Libraries**:
  - `psycopg2` - PostgreSQL adapter
  - `requests` - HTTP client for CRM APIs
  - `concurrent.futures` - Parallel processing

## Architecture

```
src/data_platform/
├── crms/               # CRM-specific integrations
│   ├── bullhorn/       # Bullhorn API client and sync logic
│   ├── hubspot/        # HubSpot integration (future)
│   └── salesforce/     # Salesforce integration (future)
├── ml/                 # ML-related functionality
│   └── embedding_generator.py  # Batch embedding generation
├── loaders/            # Data loading utilities
│   └── database_loader.py      # Bulk database operations
├── validators/         # Data quality checks
└── utils/              # Shared utilities
    ├── logger.py       # Structured logging
    └── database.py     # Database connection management
```

## Components

### Bullhorn Integration

Syncs data from Bullhorn ATS:
- **Candidates** (consultants in Bullhorn terminology)
- **Job Orders** (positions)
- **OAuth 2.0 authentication**
- **Incremental sync** based on last modified timestamps

### Embedding Generator

Generates semantic embeddings for:
- **Candidate profiles** → `profileEmbedding`
- **Job descriptions** → `descriptionEmbedding`

Features:
- **Parallel processing** with ThreadPoolExecutor
- **Batch operations** (500 records per batch)
- **Configurable workers** (default: 5 parallel threads)
- **Progress tracking** with detailed logging

### Database Loader

Bulk operations for efficient data loading:
- **Batch inserts/updates** using `executemany`
- **Transaction management**
- **Error handling and retry logic**

### Validators

Data quality checks before loading:
- **Required fields validation**
- **Data type validation**
- **Business rule validation**

## Development

### Prerequisites

- Python 3.11+
- PostgreSQL database
- Access to Bullhorn API (credentials)

### Setup

```bash
cd services/etl-service

# Create virtual environment
python -m venv venv
source venv/bin/activate

# Install dependencies
pip install -e ".[dev]"
```

### Running Components

#### Generate Embeddings

```python
from data_platform.ml.embedding_generator import EmbeddingGenerator

generator = EmbeddingGenerator(
    ml_service_url="http://localhost:8000",
    batch_size=500,
    max_workers=5
)

# Generate candidate embeddings
result = generator.generate_candidate_embeddings()
print(f"Generated {result['updated']} embeddings in {result['duration_seconds']}s")

# Generate position embeddings
result = generator.generate_position_embeddings()
```

#### Sync from Bullhorn

Typically orchestrated via Airflow DAG (see `pipelines/airflow/dags/bullhorn_sync_dag.py`)

### Environment Variables

```env
DB_HOST=localhost
DB_PORT=5432
DB_NAME=referral_system
DB_USER=referral_user
DB_PASSWORD=referral_pass

# Bullhorn OAuth
BULLHORN_CLIENT_ID=your_client_id
BULLHORN_CLIENT_SECRET=your_client_secret
BULLHORN_USERNAME=your_username
BULLHORN_PASSWORD=your_password
BULLHORN_REST_URL=https://rest.bullhornstaffing.com/rest-services/

# ML Service
ML_SERVICE_URL=http://localhost:8000
```

## Testing

```bash
# Run all tests
pytest

# Run with coverage
pytest --cov=src tests/

# Run specific test module
pytest tests/test_embedding_generator.py
```

## Orchestration with Airflow

This service is used by Airflow DAGs in `pipelines/airflow/dags/`:

```python
# Example DAG structure
with DAG('bullhorn_sync_optimized') as dag:
    sync_candidates = PythonOperator(
        task_id='sync_candidates',
        python_callable=sync_from_bullhorn
    )

    generate_embeddings = PythonOperator(
        task_id='generate_embeddings',
        python_callable=generate_candidate_embeddings
    )

    find_matches = PythonOperator(
        task_id='find_matches',
        python_callable=call_ml_service_matching
    )

    sync_candidates >> generate_embeddings >> find_matches
```

## Performance Characteristics

### Embedding Generation
- **Batch size**: 500 records
- **Parallel workers**: 5 threads
- **Typical speed**: ~100-200 records/second
- **Memory usage**: ~500MB (includes ML model)

### Bullhorn Sync
- **Page size**: 500 records per API request
- **Typical speed**: ~1000 candidates in 5-10 minutes
- **Incremental sync**: Only fetches modified records

## Data Flow

```
Bullhorn CRM
    ↓ (API sync)
ETL Service → Transform → Validate
    ↓
PostgreSQL
    ↓
Embedding Generator → ML Service
    ↓
PostgreSQL (with embeddings)
    ↓
ML Service (matching)
    ↓
Matches Table
```

## Error Handling

- **Retry logic** for transient API failures
- **Partial success tracking** - continue processing even if some records fail
- **Detailed error logging** with structured logs
- **Dead letter queue** for failed records (future enhancement)

## Monitoring

Logging structure:
```python
logger.info(
    "Processing batch",
    batch_num=1,
    total_batches=10,
    records=500,
    duration_seconds=12.5
)
```

## Deployment

Deployed as part of the Airflow worker environment:

```bash
# In Airflow container
export PYTHONPATH=/opt/airflow/services/etl-service/src
python -m data_platform.ml.embedding_generator
```

## Future Enhancements

- [ ] HubSpot CRM integration
- [ ] Salesforce CRM integration
- [ ] Real-time change data capture (CDC)
- [ ] Dead letter queue for failed syncs
- [ ] Metrics and monitoring dashboard
- [ ] Data lineage tracking

## Related Components

- **ML Service**: `services/ml-service` - Generates embeddings and matches
- **Airflow**: `pipelines/airflow` - Orchestrates ETL jobs
- **API**: `apps/api` - Consumes synced data
- **Bullhorn Mock**: `apps/bullhorn-mock` - Test Bullhorn API locally
