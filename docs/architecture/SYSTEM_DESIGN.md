# System Design & Architecture

**Referral System** - AI-Powered Candidate-Job Matching Platform

---

## Table of Contents

1. [System Overview](#system-overview)
2. [Architecture Diagram](#architecture-diagram)
3. [Component Design](#component-design)
4. [Data Flow](#data-flow)
5. [Database Design](#database-design)
6. [API Design](#api-design)
7. [ML Pipeline](#ml-pipeline)
8. [Technology Decisions](#technology-decisions)
9. [Scalability](#scalability)
10. [Security](#security)
11. [Performance Optimizations](#performance-optimizations)
12. [Deployment Architecture](#deployment-architecture)

---

## System Overview

### Purpose

Automate the candidate-to-job matching process for recruiting firms by:
- Syncing data from Bullhorn CRM/ATS
- Generating semantic embeddings for candidates and positions
- Calculating match scores using hybrid AI/rule-based algorithms
- Enabling recruiters to create and track referrals
- Providing real-time analytics and insights

### Key Metrics

- **Performance**: 10-20x faster than previous implementation
- **Scale**: Handles 500k+ candidate records
- **Accuracy**: 70+ match score threshold for quality
- **Sync Frequency**: Every 6 hours via Airflow
- **Match Time**: 30-60 seconds for full dataset

### Core Value Proposition

1. **Speed**: ML-powered bulk operations vs. sequential processing
2. **Quality**: Semantic similarity + traditional matching
3. **Automation**: End-to-end pipeline with minimal manual intervention
4. **Insights**: Real-time analytics on matches and referrals

---

## Architecture Diagram

### High-Level Architecture

```
┌─────────────────────────────────────────────────────────────────────┐
│                         External Systems                             │
│  ┌────────────────┐                                                  │
│  │  Bullhorn CRM  │ ◄─── OAuth 2.0 ────┐                           │
│  └────────────────┘                     │                           │
└──────────────▲────────────────────────┼────────────────────────────┘
               │                          │
               │ REST API                 │ REST API
               │ (Sync & Referrals)       │ (Sync & Referrals)
               │                          │
┌──────────────┴──────────────────────────┴────────────────────────────┐
│                      Application Layer                                │
│  ┌─────────────────────────────────────────────────────────────┐    │
│  │  Orchestration (Apache Airflow)                             │    │
│  │  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐     │    │
│  │  │ Sync DAG     │→ │ Embedding    │→ │ Matching     │     │    │
│  │  │ (Every 6hrs) │  │ Generation   │  │ DAG          │     │    │
│  │  └──────────────┘  └──────────────┘  └──────────────┘     │    │
│  └─────────────────────────────────────────────────────────────┘    │
│                                                                       │
│  ┌───────────────┐  ┌───────────────┐  ┌───────────────┐          │
│  │  Web Console  │  │   REST API    │  │  ML Service   │          │
│  │  (Next.js)    │  │  (Express)    │  │  (FastAPI)    │          │
│  │  Port 3000    │◄─┤  Port 3001    │◄─┤  Port 8000    │          │
│  └───────────────┘  └───────────────┘  └───────────────┘          │
│         │                   │                    │                   │
└─────────┼───────────────────┼────────────────────┼──────────────────┘
          │                   │                    │
          └───────────────────┼────────────────────┘
                              │
┌─────────────────────────────┼────────────────────────────────────────┐
│                      Data Layer                                       │
│  ┌─────────────────────────────────────────────────────────────┐    │
│  │  PostgreSQL 15 + pgvector                                    │    │
│  │  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────┐   │    │
│  │  │Candidates│  │Positions │  │ Matches  │  │Referrals │   │    │
│  │  │+ vectors │  │+ vectors │  │+ scores  │  │+ status  │   │    │
│  │  └──────────┘  └──────────┘  └──────────┘  └──────────┘   │    │
│  └─────────────────────────────────────────────────────────────┘    │
│                                                                       │
│  ┌─────────────────┐                                                │
│  │ Bullhorn Mock   │ (Development/Testing Only)                     │
│  │ (Spring Boot)   │                                                │
│  │ Port 8082       │                                                │
│  └─────────────────┘                                                │
└───────────────────────────────────────────────────────────────────────┘
```

### Component Interaction Flow

```
┌──────────┐     ┌──────────┐     ┌──────────┐     ┌──────────┐
│ Bullhorn │────▶│   ETL    │────▶│    ML    │────▶│   API    │
│   CRM    │     │ Service  │     │ Service  │     │ Service  │
└──────────┘     └──────────┘     └──────────┘     └──────────┘
                      │                 │                 │
                      ▼                 ▼                 ▼
                 ┌──────────────────────────────────────────┐
                 │         PostgreSQL + pgvector            │
                 └──────────────────────────────────────────┘
                                      ▲
                                      │
                                 ┌──────────┐
                                 │   Web    │
                                 │ Console  │
                                 └──────────┘
```

---

## Component Design

### 1. Web Console (`apps/web-consultant`)

**Technology**: Next.js 16, React 19, TypeScript

**Responsibilities**:
- User interface for recruiters
- Display matches with filtering and sorting
- Referral creation and management
- Real-time analytics dashboard
- Activity feed and notifications

**Architecture Pattern**:
- **CSR + SSR Hybrid**: Client-side for interactive features, SSR for initial load
- **State Management**: Zustand for global state, TanStack Query for server state
- **Component Structure**: Atomic design (atoms → molecules → organisms → pages)

**Key Features**:
```typescript
// State management
interface MatchStore {
  filters: MatchFilters
  selectedMatch: Match | null
  updateFilters: (filters: Partial<MatchFilters>) => void
}

// Data fetching with React Query
const { data, isLoading } = useQuery({
  queryKey: ['matches', filters],
  queryFn: () => fetchMatches(filters),
  staleTime: 30000 // Cache for 30s
})
```

**Routes**:
- `/` - Dashboard overview
- `/matches` - Match listing
- `/matches/[id]` - Match details
- `/referrals` - Referral management
- `/settings` - Configuration

---

### 2. REST API (`apps/api`)

**Technology**: Express.js, TypeScript, Prisma

**Responsibilities**:
- Serve data to frontend
- Orchestrate business logic
- Manage referrals and matches
- Provide system metrics
- Handle authentication (future)

**Architecture Pattern**: Layered Architecture

```
┌─────────────────────────────────────────┐
│  Controllers                             │  HTTP layer
│  - Request validation (Zod)            │
│  - Response formatting                 │
└─────────────┬───────────────────────────┘
              │
┌─────────────▼───────────────────────────┐
│  Services                                │  Business logic
│  - Match scoring                        │
│  - Referral creation                   │
│  - Analytics aggregation               │
└─────────────┬───────────────────────────┘
              │
┌─────────────▼───────────────────────────┐
│  Data Access (Prisma)                    │  Database layer
│  - Type-safe queries                    │
│  - Transaction management              │
└─────────────────────────────────────────┘
```

**Key Endpoints**:

```typescript
// Match endpoints
GET    /api/matches                    # List all matches
GET    /api/matches/:id                # Get single match
POST   /api/matches/find               # Trigger matching
GET    /api/matches/positions/:id/similar-candidates

// Referral endpoints
GET    /api/referrals                  # List referrals
POST   /api/referrals                  # Create referral
POST   /api/referrals/auto-create      # Bulk auto-create
POST   /api/referrals/:id/send         # Send to Bullhorn
PATCH  /api/referrals/:id/status       # Update status

// Dashboard endpoints
GET    /api/dashboard/health           # System health
GET    /api/dashboard/stats            # Statistics
GET    /api/dashboard/activity         # Recent activity
```

**Error Handling**:
```typescript
class AppError extends Error {
  statusCode: number
  isOperational: boolean

  constructor(message: string, statusCode: number) {
    super(message)
    this.statusCode = statusCode
    this.isOperational = true
  }
}

// Custom errors
class NotFoundError extends AppError {
  constructor(resource: string) {
    super(`${resource} not found`, 404)
  }
}
```

---

### 3. ML Service (`services/ml-service`)

**Technology**: Python 3.11, FastAPI, sentence-transformers, pandas

**Responsibilities**:
- Generate semantic embeddings (384-dim vectors)
- Calculate candidate-position matches
- Bulk scoring with vectorized operations
- pgvector similarity queries

**Architecture Pattern**: Service-Oriented with Repository Pattern

```python
# Service layer
class MatchingService:
    def __init__(self):
        self.db_config = {...}
        self.WEIGHTS = {...}

    def find_matches(self, position_id=None, min_score=70):
        # 1. Fetch positions
        positions_df = self.fetch_positions(position_id)

        # 2. For each position, fetch candidates (bulk query)
        candidates_df = self.fetch_candidates_for_position(...)

        # 3. Vectorized scoring (ALL at once)
        candidates_df['skill_score'] = self.score_skills_vectorized(...)
        candidates_df['experience_score'] = self.score_experience_vectorized(...)
        candidates_df['location_score'] = self.score_location_vectorized(...)

        # 4. Calculate overall score
        candidates_df['overall_score'] = weighted_sum(...)

        # 5. Filter and sort
        matches = candidates_df[candidates_df['overall_score'] >= min_score]

        # 6. Bulk upsert to database
        self.bulk_upsert_matches(matches)
```

**Performance Optimization**:
- **Bulk database queries**: Single query fetches all candidates with semantic scores
- **Vectorized operations**: pandas/numpy instead of Python loops
- **Database-level sorting**: pgvector distance calculations in PostgreSQL
- **Connection pooling**: Reuse database connections

**Matching Algorithm**:

```python
# Weights
SEMANTIC_WEIGHT = 0.60   # 60% - ML embedding similarity
SKILL_WEIGHT = 0.24      # 24% - Skill matching
EXPERIENCE_WEIGHT = 0.12 # 12% - Experience level
LOCATION_WEIGHT = 0.04   # 4% - Location compatibility

# Overall score calculation
overall_score = (
    semantic_score * SEMANTIC_WEIGHT +
    skill_score * SKILL_WEIGHT +
    experience_score * EXPERIENCE_WEIGHT +
    location_score * LOCATION_WEIGHT
)
```

**Endpoints**:
```
GET  /health                # Health check
POST /embeddings/batch      # Generate embeddings for texts
POST /match/find            # Find all matches (triggers matching)
```

---

### 4. ETL Service (`services/etl-service`)

**Technology**: Python 3.11, psycopg2, concurrent.futures

**Responsibilities**:
- Sync data from Bullhorn CRM
- Generate embeddings via ML service
- Bulk database operations
- Data validation and transformation

**Architecture Pattern**: Extract-Transform-Load Pipeline

```python
# Pipeline stages
class BullhornETL:
    def extract(self):
        """Fetch data from Bullhorn API"""
        candidates = self.bullhorn_client.search_candidates(...)
        positions = self.bullhorn_client.search_job_orders(...)
        return candidates, positions

    def transform(self, data):
        """Clean and normalize data"""
        # Normalize skills (lowercase, synonyms)
        # Parse dates and experience
        # Validate required fields
        return cleaned_data

    def load(self, data):
        """Bulk insert/update to database"""
        self.database_loader.bulk_upsert(data)
```

**Embedding Generation**:
```python
class EmbeddingGenerator:
    def __init__(self, batch_size=500, max_workers=5):
        self.batch_size = batch_size
        self.max_workers = max_workers

    def generate_candidate_embeddings(self):
        # 1. Fetch candidates without embeddings
        candidates = fetch_candidates_without_embeddings()

        # 2. Split into batches
        batches = chunk(candidates, self.batch_size)

        # 3. Process in parallel
        with ThreadPoolExecutor(max_workers=self.max_workers) as executor:
            futures = [
                executor.submit(self.process_batch, batch)
                for batch in batches
            ]

            # 4. Collect results
            for future in as_completed(futures):
                result = future.result()
                log_progress(result)
```

**Performance Characteristics**:
- **Batch size**: 500 records per batch
- **Parallel workers**: 5 threads
- **Speed**: ~100-200 embeddings/second
- **Memory**: ~500MB (includes ML model in memory)

---

### 5. Orchestration (`pipelines/airflow`)

**Technology**: Apache Airflow 2.8

**Responsibilities**:
- Schedule ETL jobs
- Monitor pipeline execution
- Handle retries and failures
- Track execution history

**DAG Structure**:

```python
# Main sync DAG - runs every 6 hours
with DAG('bullhorn_sync_optimized',
         schedule_interval='0 */6 * * *',
         catchup=False) as dag:

    # Task 1: Sync candidates from Bullhorn
    sync_candidates = PythonOperator(
        task_id='sync_candidates',
        python_callable=sync_candidates_from_bullhorn,
        retries=3,
        retry_delay=timedelta(minutes=5)
    )

    # Task 2: Generate embeddings for new candidates
    generate_candidate_embeddings = PythonOperator(
        task_id='generate_candidate_embeddings',
        python_callable=generate_embeddings
    )

    # Task 3: Sync positions
    sync_positions = PythonOperator(
        task_id='sync_positions',
        python_callable=sync_positions_from_bullhorn
    )

    # Task 4: Generate position embeddings
    generate_position_embeddings = PythonOperator(
        task_id='generate_position_embeddings',
        python_callable=generate_position_embeddings
    )

    # Task 5: Run matching algorithm
    find_matches = PythonOperator(
        task_id='find_matches',
        python_callable=trigger_ml_matching,
        execution_timeout=timedelta(minutes=30)
    )

    # Define dependencies
    sync_candidates >> generate_candidate_embeddings
    generate_candidate_embeddings >> sync_positions
    sync_positions >> generate_position_embeddings
    generate_position_embeddings >> find_matches
```

**Retry Logic**:
- 3 retries with 5-minute delay
- Exponential backoff for API failures
- Email notifications on final failure

---

## Data Flow

### 1. Data Sync Flow

```
┌────────────┐
│  Bullhorn  │
│    CRM     │
└─────┬──────┘
      │ API Call
      │ (OAuth 2.0)
      ▼
┌────────────────┐
│   ETL Service  │
│  - Fetch data  │
│  - Transform   │
│  - Validate    │
└────────┬───────┘
         │ SQL INSERT/UPDATE
         ▼
┌────────────────┐
│   PostgreSQL   │
│  - Candidates  │
│  - Positions   │
└────────────────┘
```

### 2. Embedding Generation Flow

```
┌────────────────┐
│   PostgreSQL   │
│  (raw data)    │
└────────┬───────┘
         │ SELECT records without embeddings
         ▼
┌────────────────┐
│  ETL Service   │
│  - Batch read  │
│  - Build text  │
└────────┬───────┘
         │ HTTP POST /embeddings/batch
         ▼
┌────────────────┐
│   ML Service   │
│ - Transformer  │
│ - Generate vec │
└────────┬───────┘
         │ Return vectors
         ▼
┌────────────────┐
│  ETL Service   │
│  - Bulk update │
└────────┬───────┘
         │ UPDATE with vectors
         ▼
┌────────────────┐
│   PostgreSQL   │
│  (+ vectors)   │
└────────────────┘
```

### 3. Matching Flow

```
┌────────────────┐
│   Airflow DAG  │
│  (scheduler)   │
└────────┬───────┘
         │ Trigger every 6 hours
         ▼
┌────────────────┐
│   ML Service   │
│ /match/find    │
└────────┬───────┘
         │
         ▼
    ┌─────────────────────────────────┐
    │ For each open position:         │
    │                                 │
    │ 1. Fetch position + embedding   │
    │ 2. Query candidates with        │
    │    pgvector similarity          │
    │ 3. Score all candidates         │
    │    (vectorized operations)      │
    │ 4. Filter by min_score (70+)   │
    │ 5. Take top 100 matches         │
    └──────────┬──────────────────────┘
               │
               ▼
┌────────────────────────────────────┐
│     Bulk INSERT matches            │
│     (ON CONFLICT UPDATE)           │
└──────────┬─────────────────────────┘
           │
           ▼
┌────────────────┐
│   PostgreSQL   │
│  Match table   │
└────────────────┘
```

### 4. User Interaction Flow

```
┌────────────┐
│    User    │
│  (Browser) │
└─────┬──────┘
      │ HTTP GET /matches
      ▼
┌────────────────┐
│  Next.js App   │
│  (Web Console) │
└────────┬───────┘
         │ API call
         ▼
┌────────────────┐
│   REST API     │
│  /api/matches  │
└────────┬───────┘
         │ Prisma query
         ▼
┌────────────────┐
│   PostgreSQL   │
│  SELECT with   │
│  JOIN, filters │
└────────┬───────┘
         │ Return results
         ▼
┌────────────────┐
│   REST API     │
│  Format JSON   │
└────────┬───────┘
         │
         ▼
┌────────────────┐
│  Next.js App   │
│  Render UI     │
└────────┬───────┘
         │
         ▼
┌────────────┐
│    User    │
│  See data  │
└────────────┘
```

---

## Database Design

### Entity Relationship Diagram

```
┌─────────────┐         ┌─────────────┐
│  Candidate  │         │  Position   │
│─────────────│         │─────────────│
│ id (PK)     │         │ id (PK)     │
│ bullhornId  │         │ bullhornId  │
│ firstName   │         │ title       │
│ lastName    │         │ description │
│ email       │         │ reqSkills[] │
│ skills[]    │         │ prefSkills[]│
│ experience  │         │ expLevel    │
│ profileEmb  │◄────┐   │ descEmb     │
│ status      │     │   │ status      │
│ createdAt   │     │   │ createdAt   │
└─────┬───────┘     │   └─────┬───────┘
      │             │         │
      │             │         │
      │    ┌────────┴─────────┴────────┐
      │    │        Match               │
      │    │────────────────────────    │
      └───►│ candidateId (FK)           │
           │ positionId (FK)            │◄──┐
           │ overallScore               │   │
           │ semanticScore              │   │
           │ skillMatchScore            │   │
           │ experienceScore            │   │
           │ locationScore              │   │
           │ matchedSkills[]            │   │
           │ missingSkills[]            │   │
           │ status                     │   │
           │ createdAt                  │   │
           └────────┬───────────────────┘   │
                    │                       │
                    │                       │
           ┌────────▼───────────────────┐   │
           │       Referral             │   │
           │────────────────────────    │   │
           │ id (PK)                    │   │
           │ candidateId (FK)           │───┘
           │ positionId (FK)            │
           │ consultantId (FK)          │
           │ status                     │
           │ sentToBullhorn             │
           │ bullhornSubmissionId       │
           │ notes                      │
           │ createdAt                  │
           └────────────────────────────┘
```

### Key Tables

#### Candidate
```sql
CREATE TABLE "Candidate" (
  id                TEXT PRIMARY KEY,
  "bullhornId"      TEXT UNIQUE NOT NULL,
  "firstName"       TEXT NOT NULL,
  "lastName"        TEXT NOT NULL,
  email             TEXT,
  phone             TEXT,
  skills            TEXT[],
  experience        JSONB,
  "currentTitle"    TEXT,
  summary           TEXT,
  "profileEmbedding" vector(384),  -- pgvector type
  location          TEXT,
  status            TEXT DEFAULT 'ACTIVE',
  "createdAt"       TIMESTAMP DEFAULT NOW(),
  "updatedAt"       TIMESTAMP DEFAULT NOW()
);

-- Indexes for performance
CREATE INDEX idx_candidate_status ON "Candidate"(status);
CREATE INDEX idx_candidate_skills ON "Candidate" USING GIN(skills);
CREATE INDEX idx_candidate_embedding ON "Candidate"
  USING ivfflat ("profileEmbedding" vector_cosine_ops);
```

#### Position
```sql
CREATE TABLE "Position" (
  id                      TEXT PRIMARY KEY,
  "bullhornId"            TEXT UNIQUE NOT NULL,
  title                   TEXT NOT NULL,
  description             TEXT,
  "requiredSkills"        TEXT[],
  "preferredSkills"       TEXT[],
  "experienceLevel"       TEXT,
  location                TEXT,
  "descriptionEmbedding"  vector(384),
  status                  TEXT DEFAULT 'OPEN',
  "createdAt"             TIMESTAMP DEFAULT NOW(),
  "updatedAt"             TIMESTAMP DEFAULT NOW()
);

-- Indexes
CREATE INDEX idx_position_status ON "Position"(status);
CREATE INDEX idx_position_embedding ON "Position"
  USING ivfflat ("descriptionEmbedding" vector_cosine_ops);
```

#### Match
```sql
CREATE TABLE "Match" (
  id                TEXT PRIMARY KEY,
  "candidateId"     TEXT REFERENCES "Candidate"(id),
  "positionId"      TEXT REFERENCES "Position"(id),
  "overallScore"    INTEGER NOT NULL,
  "semanticScore"   INTEGER,
  "skillMatchScore" INTEGER,
  "experienceScore" INTEGER,
  "locationScore"   INTEGER,
  "matchedSkills"   TEXT[],
  "missingSkills"   TEXT[],
  "matchReason"     TEXT,
  status            TEXT DEFAULT 'PENDING',
  "createdAt"       TIMESTAMP DEFAULT NOW(),
  "updatedAt"       TIMESTAMP DEFAULT NOW(),

  UNIQUE("candidateId", "positionId")
);

-- Indexes
CREATE INDEX idx_match_candidate ON "Match"("candidateId");
CREATE INDEX idx_match_position ON "Match"("positionId");
CREATE INDEX idx_match_score ON "Match"("overallScore" DESC);
CREATE INDEX idx_match_status ON "Match"(status);
```

### Database Optimization Strategies

1. **Vector Indexing (IVFFlat)**
   - Fast approximate nearest neighbor search
   - Trade-off: 98% accuracy for 100x speed

2. **Composite Indexes**
   - `(status, overallScore)` for filtered queries
   - `(candidateId, positionId)` for duplicate prevention

3. **Partial Indexes**
   ```sql
   CREATE INDEX idx_active_candidates
   ON "Candidate"(id) WHERE status = 'ACTIVE';
   ```

4. **Connection Pooling**
   - Prisma: `connection_limit=10`
   - Python: psycopg2 pool

5. **Query Optimization**
   - Use `SELECT` specific fields, not `*`
   - Batch operations with `executemany`
   - Prepared statements for repeated queries

---

## API Design

### RESTful Principles

1. **Resource-based URLs**
   - `/api/matches` (collection)
   - `/api/matches/:id` (resource)
   - `/api/getMatch` (verb-based)

2. **HTTP Methods**
   - `GET` - Read
   - `POST` - Create
   - `PUT/PATCH` - Update
   - `DELETE` - Delete

3. **Status Codes**
   - `200 OK` - Success
   - `201 Created` - Resource created
   - `400 Bad Request` - Invalid input
   - `404 Not Found` - Resource doesn't exist
   - `500 Internal Server Error` - Server error

### Request/Response Format

**Request Example**:
```http
GET /api/matches?status=PENDING&minScore=80&page=1&limit=20
Authorization: Bearer <token>
```

**Response Example**:
```json
{
  "success": true,
  "data": {
    "matches": [
      {
        "id": "match_123",
        "candidateId": "cand_456",
        "positionId": "pos_789",
        "overallScore": 85,
        "semanticScore": 88,
        "skillMatchScore": 82,
        "experienceScore": 90,
        "locationScore": 75,
        "matchedSkills": ["JavaScript", "React", "Node.js"],
        "missingSkills": ["AWS"],
        "status": "PENDING",
        "createdAt": "2026-01-12T10:00:00Z"
      }
    ],
    "pagination": {
      "page": 1,
      "limit": 20,
      "total": 150,
      "pages": 8
    }
  },
  "metadata": {
    "timestamp": "2026-01-12T10:00:00Z",
    "requestId": "req_abc123"
  }
}
```

**Error Response**:
```json
{
  "success": false,
  "error": {
    "code": "VALIDATION_ERROR",
    "message": "Invalid match score",
    "details": {
      "field": "minScore",
      "constraint": "Must be between 0 and 100"
    }
  },
  "metadata": {
    "timestamp": "2026-01-12T10:00:00Z",
    "requestId": "req_abc123"
  }
}
```

### Validation with Zod

```typescript
import { z } from 'zod'

const MatchFiltersSchema = z.object({
  status: z.enum(['PENDING', 'REVIEWED', 'REFERRED']).optional(),
  minScore: z.number().min(0).max(100).optional(),
  candidateId: z.string().optional(),
  positionId: z.string().optional(),
  page: z.number().min(1).default(1),
  limit: z.number().min(1).max(100).default(20)
})

// In controller
export const getMatches = async (req, res) => {
  const filters = MatchFiltersSchema.parse(req.query)
  // ... fetch matches
}
```

---

## ML Pipeline

### Embedding Generation

**Model**: sentence-transformers `all-MiniLM-L6-v2`
- **Dimensions**: 384
- **Size**: ~23MB
- **Speed**: ~1000 sentences/second on CPU
- **Quality**: Good balance of speed and accuracy

**Text Construction**:

```python
# For candidates
profile_text = f"""
{first_name} {last_name}.
Current Title: {current_title}.
Summary: {summary}.
Skills: {', '.join(skills)}.
"""

# For positions
job_text = f"""
{title}.
Description: {description}.
Required Skills: {', '.join(required_skills)}.
Preferred Skills: {', '.join(preferred_skills)}.
Experience Level: {experience_level}.
Location: {location}.
"""
```

**Embedding Process**:
```python
from sentence_transformers import SentenceTransformer

model = SentenceTransformer('all-MiniLM-L6-v2')
embeddings = model.encode(texts, batch_size=32)
# Returns: numpy array of shape (n, 384)
```

### Matching Algorithm Details

**1. Semantic Similarity (60%)**
```python
# Using pgvector cosine distance
similarity = 1 - (candidate_embedding <=> position_embedding)
semantic_score = similarity * 100
```

**2. Skill Matching (24%)**
```python
def score_skills(candidate_skills, required_skills, preferred_skills):
    # Normalize with synonyms
    candidate_canonical = [canonicalize(s) for s in candidate_skills]

    # Count matches
    required_matched = count_matches(required_skills, candidate_canonical)
    preferred_matched = count_matches(preferred_skills, candidate_canonical)

    # Weighted score (required: 70%, preferred: 30%)
    score = (
        (required_matched / len(required_skills)) * 0.7 +
        (preferred_matched / len(preferred_skills)) * 0.3
    ) * 100

    return score
```

**3. Experience Matching (12%)**
```python
EXPERIENCE_LEVELS = {
    'ENTRY': 1,    # <2 years
    'MID': 2,      # 2-5 years
    'SENIOR': 3,   # 5-8 years
    'LEAD': 4,     # 8-12 years
    'EXECUTIVE': 5 # 12+ years
}

def score_experience(candidate_years, required_level):
    candidate_level = years_to_level(candidate_years)
    required_level_num = EXPERIENCE_LEVELS[required_level]

    diff = abs(candidate_level - required_level_num)

    if diff == 0: return 100
    if diff == 1: return 75
    if diff == 2: return 50
    return 25
```

**4. Location Matching (4%)**
```python
def score_location(candidate_loc, position_loc):
    if 'remote' in candidate_loc.lower() or 'remote' in position_loc.lower():
        return 100  # Remote = always match

    if candidate_loc.lower() == position_loc.lower():
        return 100  # Exact match

    # Parse city, state
    cand_city, cand_state = parse_location(candidate_loc)
    pos_city, pos_state = parse_location(position_loc)

    if cand_city == pos_city:
        return 80  # Same city

    if cand_state == pos_state:
        return 60  # Same state

    return 30  # Different location
```

**Overall Score**:
```python
overall_score = (
    semantic_score * 0.60 +
    skill_score * 0.24 +
    experience_score * 0.12 +
    location_score * 0.04
)
```

---

## Technology Decisions

### Why These Technologies?

| Technology | Reason |
|------------|--------|
| **Next.js** | SSR + CSR hybrid, excellent DX, production-ready |
| **TypeScript** | Type safety, better IDE support, catch errors early |
| **Express** | Mature, flexible, large ecosystem |
| **Prisma** | Type-safe ORM, great migrations, auto-completion |
| **FastAPI** | Fast (async), automatic docs, Python type hints |
| **sentence-transformers** | SOTA embeddings, pre-trained models, easy to use |
| **pgvector** | Vector similarity in PostgreSQL (no separate vector DB) |
| **PostgreSQL** | Robust, ACID compliant, JSON support, extensions |
| **Airflow** | Battle-tested orchestration, rich UI, extensible |
| **Docker** | Consistent environments, easy deployment |

### Alternative Considerations

| Instead Of | Why Not? |
|------------|----------|
| **Pinecone/Weaviate** (vector DB) | pgvector sufficient for our scale, simpler architecture |
| **GraphQL** | REST simpler for this use case, no complex nested queries |
| **MongoDB** | Need ACID transactions, relational data model |
| **Kubernetes** | Overkill for current scale, Docker Compose sufficient |
| **Redis** | Not needed yet, PostgreSQL performance sufficient |

---

## Scalability

### Current Capacity

- **Candidates**: 500k+ records
- **Positions**: 10k+ records
- **Matches**: 5M+ potential combinations
- **Processing Time**: 30-60 seconds for full matching
- **Concurrent Users**: ~50 (current)

### Horizontal Scaling Strategy

```
┌─────────────────────────────────────────────────────────┐
│                    Load Balancer (Nginx)                │
└────────┬──────────────┬──────────────┬──────────────────┘
         │              │              │
    ┌────▼────┐    ┌────▼────┐    ┌────▼────┐
    │ API 1   │    │ API 2   │    │ API 3   │
    └────┬────┘    └────┬────┘    └────┬────┘
         │              │              │
         └──────────────┴──────────────┘
                        │
         ┌──────────────▼──────────────┐
         │  PostgreSQL Primary          │
         └──────────────┬──────────────┘
                        │
         ┌──────────────▼──────────────┐
         │  PostgreSQL Read Replicas   │
         └─────────────────────────────┘
```

### Vertical Scaling (Current Approach)

**Database Optimization**:
- Increase connection pool size
- Optimize queries with EXPLAIN ANALYZE
- Add strategic indexes
- Use materialized views for analytics

**ML Service**:
- Increase batch size (500 → 1000)
- More parallel workers (5 → 10)
- GPU acceleration (if available)

### Caching Strategy (Future)

```
┌────────┐     ┌────────┐     ┌────────────┐
│  User  │────▶│ Redis  │────▶│ PostgreSQL │
└────────┘     │ Cache  │     └────────────┘
               └────────┘

Cache Keys:
- matches:candidate:{id}    (TTL: 5 min)
- matches:position:{id}     (TTL: 5 min)
- dashboard:stats           (TTL: 1 min)
```

### Database Sharding (Future)

If we reach 10M+ candidates:

```
Shard by candidate ID:
- Shard 1: IDs starting with 0-4
- Shard 2: IDs starting with 5-9
- Shard 3: IDs starting with a-f
```

---

## Security

### Current Implementation

1. **Database Security**
   - Parameterized queries (SQL injection prevention)
   - Least privilege database users
   - Environment variables for credentials

2. **API Security**
   - Input validation (Zod)
   - Rate limiting (future)
   - Authentication (future - JWT)
   - Authorization (future - RBAC)

3. **Network Security**
   - Docker network isolation
   - HTTPS in production (future)
   - No exposed database ports (Docker internal)

### Security Roadmap

**Phase 1: Authentication**
```typescript
// JWT-based auth
POST /api/auth/login
POST /api/auth/logout
POST /api/auth/refresh

// Protected routes
GET /api/matches
Authorization: Bearer <jwt_token>
```

**Phase 2: Authorization (RBAC)**
```typescript
enum Role {
  ADMIN = 'admin',
  RECRUITER = 'recruiter',
  VIEWER = 'viewer'
}

const permissions = {
  admin: ['*'],
  recruiter: ['read:matches', 'write:referrals'],
  viewer: ['read:matches']
}
```

**Phase 3: Data Privacy**
- PII encryption at rest
- Audit logging for data access
- GDPR compliance (data deletion)

---

## Performance Optimizations

### 1. Database Query Optimization

**Before**:
```typescript
// N+1 query problem
const matches = await prisma.match.findMany()
for (const match of matches) {
  const candidate = await prisma.candidate.findUnique({
    where: { id: match.candidateId }
  })
  const position = await prisma.position.findUnique({
    where: { id: match.positionId }
  })
}
```

**After**:
```typescript
// Single query with joins
const matches = await prisma.match.findMany({
  include: {
    candidate: true,
    position: true
  }
})
```

### 2. Bulk Operations

**Python ML Service**:
```python
# Before: Loop (slow)
for candidate in candidates:
    score = calculate_skill_score(candidate.skills, required_skills)
    scores.append(score)

# After: Vectorized (fast)
candidates_df['skill_score'] = candidates_df['skills'].apply(
    lambda skills: calculate_skill_score(skills, required_skills)
)
```

### 3. Pagination

```typescript
// Efficient pagination with cursor
const matches = await prisma.match.findMany({
  take: 20,
  skip: (page - 1) * 20,
  orderBy: { createdAt: 'desc' }
})
```

### 4. Caching (Future)

```typescript
// Redis cache layer
const cacheKey = `matches:position:${positionId}`
const cached = await redis.get(cacheKey)

if (cached) {
  return JSON.parse(cached)
}

const matches = await fetchMatchesFromDB(positionId)
await redis.set(cacheKey, JSON.stringify(matches), 'EX', 300) // 5 min TTL
```

---

## Deployment Architecture

### Development Environment

```
Docker Compose (Local)
├── PostgreSQL (port 5433)
├── pgAdmin (port 5051)
├── ML Service (port 8000)
├── Airflow Webserver (port 8081)
├── Airflow Scheduler
└── Bullhorn Mock (port 8082)

Separate processes:
├── API (npm run dev) - port 3001
└── Web Console (npm run dev) - port 3000
```

### Production Environment (Future)

```
                        ┌──────────────┐
                        │     CDN      │
                        │  (CloudFlare)│
                        └──────┬───────┘
                               │
                        ┌──────▼───────┐
                        │     Nginx    │
                        │ Load Balancer│
                        └──────┬───────┘
                               │
                ┌──────────────┼──────────────┐
                │              │              │
         ┌──────▼──────┐ ┌────▼──────┐ ┌────▼──────┐
         │   Web App   │ │    API    │ │ML Service │
         │  (Vercel)   │ │  (EC2)    │ │  (EC2)    │
         └─────────────┘ └─────┬─────┘ └─────┬─────┘
                               │              │
                         ┌─────▼──────────────▼─────┐
                         │    PostgreSQL (RDS)      │
                         │    + Read Replicas       │
                         └──────────────────────────┘

                         ┌──────────────────────────┐
                         │   Airflow (ECS/Fargate)  │
                         └──────────────────────────┘
```

### CI/CD Pipeline (Future)

```
GitHub Push
    ↓
GitHub Actions
    ├─→ Run tests
    ├─→ Build Docker images
    ├─→ Push to ECR
    └─→ Deploy to ECS

Deployment Strategy:
- Blue-Green deployment
- Health checks before traffic switch
- Automatic rollback on failure
```

---

## Monitoring & Observability (Future)

### Metrics to Track

**Application Metrics**:
- API response times (p50, p95, p99)
- Error rates by endpoint
- Matching job duration
- Embedding generation speed
- Database query performance

**Business Metrics**:
- Matches created per day
- Referrals sent per day
- Average match score
- Conversion rate (match → referral)

**Infrastructure Metrics**:
- CPU/Memory usage
- Database connections
- Disk I/O
- Network throughput

### Logging

**Structured Logging**:
```typescript
logger.info('Match created', {
  matchId: match.id,
  candidateId: match.candidateId,
  positionId: match.positionId,
  score: match.overallScore,
  requestId: req.id
})
```

**Log Levels**:
- `ERROR` - System errors, exceptions
- `WARN` - Degraded performance, retries
- `INFO` - Important events (match created, referral sent)
- `DEBUG` - Detailed trace information

---

## Conclusion

This system demonstrates a modern, scalable architecture for AI-powered recruitment automation:

- **Performance**: 10-20x improvement through vectorized operations
- **Accuracy**: Hybrid ML + rule-based matching
- **Scalability**: Ready for 500k+ records, extensible to millions
- **Maintainability**: Clean architecture, comprehensive documentation
- **Developer Experience**: Type-safe, well-tested, easy to onboard

### Future Enhancements

1. **Authentication & Authorization** - JWT + RBAC
2. **Real-time Updates** - WebSockets for live dashboard
3. **Advanced Analytics** - ML-powered insights and predictions
4. **Multi-tenancy** - Support multiple organizations
5. **Mobile App** - React Native for recruiters on the go
6. **Integration Hub** - Connect to more ATS/CRM systems

---

For detailed component documentation, see:
- [apps/api/README.md](../apps/api/README.md)
- [services/ml-service/README.md](../services/ml-service/README.md)
- [services/etl-service/README.md](../services/etl-service/README.md)
- [pipelines/airflow/README.md](../pipelines/airflow/README.md)
