# ML Service

Machine Learning service for semantic candidate-position matching using embeddings.

## Purpose

Provides high-performance candidate matching using:
- **Semantic similarity** via sentence embeddings (60% weight)
- **Skill matching** with synonym normalization (24% weight)
- **Experience matching** based on years/level (12% weight)
- **Location matching** with remote support (4% weight)

## Tech Stack

- **Language**: Python 3.11+
- **Framework**: FastAPI + Uvicorn
- **ML Library**: sentence-transformers (all-MiniLM-L6-v2)
- **Database**: PostgreSQL with pgvector extension
- **Data Processing**: pandas, numpy

## Performance

- **10-20x faster** than previous TypeScript implementation
- **Bulk operations** using pgvector for similarity calculations
- **Vectorized scoring** with pandas for all candidates at once
- **Typical speed**: 30-60 seconds for full dataset matching

## Architecture

```
src/
├── main.py          # FastAPI application & endpoints
└── matching.py      # MatchingService class with scoring logic
```

## API Endpoints

### Health Check
```bash
GET /health
Response: {"status": "healthy", "model": "all-MiniLM-L6-v2", "dimension": 384}
```

### Generate Embeddings
```bash
POST /embeddings/batch
Body: {"texts": ["text1", "text2", ...]}
Response: {"embeddings": [[...], [...], ...]}
```

### Find Matches
```bash
POST /match/find
Body: {"minScore": 70, "positionId": "optional"}
Response: {
  "positionsProcessed": 10,
  "totalMatches": 150,
  "durationSeconds": 45.2
}
```

## Development

### Prerequisites

- Python 3.11+
- PostgreSQL with pgvector extension
- ~500MB for sentence-transformers model

### Setup

```bash
cd services/ml-service

# Create virtual environment
python -m venv venv
source venv/bin/activate  # or `venv\Scripts\activate` on Windows

# Install dependencies
pip install -r requirements.txt

# Install in development mode
pip install -e ".[dev]"
```

### Running Locally

```bash
# Run the service
uvicorn src.main:app --host 0.0.0.0 --port 8000 --reload

# With environment variables
export DB_HOST=localhost
export DB_PORT=5432
export DB_NAME=referral_system
export DB_USER=referral_user
export DB_PASSWORD=referral_pass

uvicorn src.main:app --reload
```

### Environment Variables

```env
DB_HOST=localhost
DB_PORT=5432
DB_NAME=referral_system
DB_USER=referral_user
DB_PASSWORD=referral_pass
PYTHONUNBUFFERED=1
```

## Testing

```bash
# Run tests
pytest

# Run with coverage
pytest --cov=src tests/

# Run specific test
pytest tests/test_matching.py
```

## Docker

### Build

```bash
docker build -t ml-service .
```

### Run

```bash
docker run -p 8000:8000 \
  -e DB_HOST=postgres \
  -e DB_PORT=5432 \
  -e DB_NAME=referral_system \
  -e DB_USER=referral_user \
  -e DB_PASSWORD=referral_pass \
  ml-service
```

## Matching Algorithm

### Scoring Weights

| Component | Weight | Description |
|-----------|--------|-------------|
| Semantic | 60% | Embedding similarity between job description and candidate profile |
| Skills | 24% | Match ratio of required and preferred skills |
| Experience | 12% | Years of experience vs required level |
| Location | 4% | Geographic match (100 for remote) |

### Skill Matching Logic

- **Required skills**: 70% of skill score weight
- **Preferred skills**: 30% of skill score weight
- **Synonym normalization**: "javascript" matches "js", "es6", etc.
- **Case-insensitive matching**

### Experience Matching Logic

| Years | Level |
|-------|-------|
| <2 | ENTRY |
| 2-5 | MID |
| 5-8 | SENIOR |
| 8-12 | LEAD |
| 12+ | EXECUTIVE |

Score based on level difference:
- Same level: 100
- 1 level difference: 75
- 2 level difference: 50
- 3+ level difference: 25

### Location Matching Logic

- Remote: 100 (always matches)
- Exact match: 100
- Same city: 80
- Same state: 60
- Different: 30

## Integration with Airflow

This service is called from the Bullhorn sync DAG:

```python
# In Airflow DAG
response = requests.post(
    "http://ml-service:8000/match/find",
    json={"minScore": 70}
)
```

## Deployment

Deployed as part of the docker-compose stack:

```yaml
ml-service:
  build: ./services/ml-service
  ports:
    - "8000:8000"
  environment:
    - DB_HOST=postgres
```

## Performance Optimization

- **Bulk database queries** using pgvector distance calculations
- **Vectorized scoring** with pandas (no Python loops)
- **Model caching** - sentence-transformers model loaded at startup
- **Connection pooling** for database connections
- **Parallel embedding generation** (future enhancement)

## Related Components

- **ETL Service**: `services/etl-service` - Generates embeddings
- **Airflow**: `pipelines/airflow` - Orchestrates matching jobs
- **API**: `apps/api` - Serves match results to frontend
