# Developer Onboarding Guide - Referral System

Complete guide for setting up and running the Referral System for new developers.

---

## Table of Contents

1. [System Overview](#system-overview)
2. [Prerequisites](#prerequisites)
3. [Quick Start Guide](#quick-start-guide)
4. [Architecture](#architecture)
5. [Technology Stack](#technology-stack)
6. [Service Ports Reference](#service-ports-reference)
7. [Environment Configuration](#environment-configuration)
8. [Docker Services](#docker-services)
9. [Database Setup](#database-setup)
10. [NPM Scripts Reference](#npm-scripts-reference)
11. [Development Workflow](#development-workflow)
12. [API Endpoints](#api-endpoints)
13. [Testing the System](#testing-the-system)
14. [Key Files & Directories](#key-files--directories)
15. [Troubleshooting](#troubleshooting)
16. [Monitoring & Debugging](#monitoring--debugging)
17. [Switching to Real Bullhorn](#switching-to-real-bullhorn)

---

## System Overview

The **Referral System** is an AI-powered platform that automatically matches candidates to job positions and manages referrals to the Bullhorn ATS (Applicant Tracking System). It uses machine learning embeddings for semantic matching and provides a consultant dashboard for managing the referral process.

### Key Features

- **Automated Candidate-Job Matching** using ML embeddings
- **Bullhorn Integration** (with mock service for development)
- **Semantic Search** powered by pgvector
- **ETL Pipeline** using Apache Airflow
- **Real-time Dashboard** for consultants
- **RESTful API** for all operations

### Architecture Type

This is a **monorepo** managed by **Turborepo** with the following components:

- **3 Applications**: API (Express), Dashboard (Next.js), ML Service (FastAPI)
- **2 Shared Packages**: Database (Prisma), Shared utilities
- **1 External Service**: Bullhorn Mock (Java Spring Boot)
- **1 Data Pipeline**: Apache Airflow with Python ETL modules
- **Infrastructure**: PostgreSQL, LocalStack, pgAdmin

---

## Prerequisites

### Required Software

- **Node.js** 20+ ([Download](https://nodejs.org/))
- **npm** 10+ (comes with Node.js)
- **Docker Desktop** ([Download](https://www.docker.com/products/docker-desktop))
- **Git** ([Download](https://git-scm.com/))

### Optional (for specific development)

- **Python** 3.11+ (for ML service development)
- **Java** 21+ (for Bullhorn mock development)
- **PostgreSQL Client** (for direct database access)

### System Requirements

- **RAM**: 8GB minimum, 16GB recommended
- **Disk Space**: 10GB free space
- **OS**: macOS, Linux, or Windows with WSL2

---

## Quick Start Guide

### Step 1: Clone and Install

```bash
# Clone the repository
git clone <repository-url>
cd referral-system

# Install all dependencies (uses npm workspaces)
npm install
```

### Step 2: Environment Setup

```bash
# Copy environment template
cp .env.example .env

# Edit .env if needed (defaults work for development)
# Default configuration uses mock Bullhorn service
```

**Key Environment Variables** (defaults work out of the box):
```env
PORT=3000
NODE_ENV=development
DATABASE_URL="postgresql://referral_user:referral_pass@localhost:5433/referral_system?schema=public"
BULLHORN_MOCK_MODE=true
BULLHORN_MOCK_BASE_URL=http://localhost:8082
```

### Step 3: Start Infrastructure

```bash
# Start all Docker services (Postgres, Bullhorn Mock, ML Service, Airflow, etc.)
npm run infra:up

# Wait ~60 seconds for all containers to be healthy
# Check status with:
docker-compose -f infrastructure/docker/docker-compose.yml ps
```

**Expected Output**: All services should show `Up` status with `(healthy)` indicator.

### Step 4: Database Setup

```bash
# Generate Prisma client from schema
npm run db:generate

# Run database migrations (creates all tables)
npm run db:migrate

# Seed test data (consultants, candidates, positions)
npm run db:seed
```

### Step 5: Start Development Servers

```bash
# Start all applications in development mode
npm run dev

# This starts:
# - API server on http://localhost:3001
# - Consultant Dashboard on http://localhost:3000 (Next.js)
# - All supporting services
```

### Step 6: Verify Setup

```bash
# Check API health
curl http://localhost:3001/api/dashboard/health

# Expected response:
# {"status":"ok","database":"connected","redis":"connected"}

# Check system stats
curl http://localhost:3001/api/dashboard/stats
```

**Access Points**:
- API: http://localhost:3001/api
- Dashboard: http://localhost:3000
- Airflow UI: http://localhost:8081 (admin/admin)
- pgAdmin: http://localhost:5051 (admin@admin.com/admin)
- Prisma Studio: `npm run db:studio` → http://localhost:5555

**Setup Complete!** 

---

## Architecture

### System Architecture Diagram

```
┌────────────────────────────────────────────────────────────────┐
│                        EXTERNAL SERVICES                        │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │  Bullhorn CRM/ATS (or Mock Service - Java Spring Boot)  │  │
│  │  Port 8082 - Simulates full Bullhorn API                │  │
│  └────────────────────┬─────────────────────────────────────┘  │
└───────────────────────┼────────────────────────────────────────┘
                        │ REST API (OAuth 2.0)
                        │
┌───────────────────────▼────────────────────────────────────────┐
│                   REFERRAL SYSTEM API                          │
│                   (Express.js - Port 3001)                     │
│  ┌──────────────────┐  ┌──────────────────┐  ┌─────────────┐ │
│  │  Sync Service    │  │ Matching Service │  │  Referral   │ │
│  │  - Fetch from    │  │ - Score candidates│  │  Service    │ │
│  │    Bullhorn      │  │ - ML embeddings   │  │ - Create &  │ │
│  │  - Transform     │  │ - Rank matches    │  │   send      │ │
│  │  - Store data    │  │                   │  │             │ │
│  └──────────────────┘  └──────────────────┘  └─────────────┘ │
└───────┬────────────────────┬────────────────────┬─────────────┘
        │                    │                    │
        │                    │                    │
┌───────▼──────┐  ┌──────────▼─────────┐  ┌──────▼──────────────┐
│  PostgreSQL  │  │   ML Service       │  │  Consultant         │
│  + pgvector  │  │   (FastAPI)        │  │  Dashboard          │
│              │  │   Port 8000        │  │  (Next.js)          │
│  - Main DB   │  │                    │  │                     │
│    Port 5433 │  │  - Embeddings      │  │  - View matches     │
│  - Bullhorn  │  │  - Semantic search │  │  - Create referrals │
│    Port 5434 │  │  - Model:          │  │  - Track activity   │
│              │  │    MiniLM-L6-v2    │  │                     │
└──────────────┘  └────────────────────┘  └─────────────────────┘

┌─────────────────────────────────────────────────────────────────┐
│                      DATA PIPELINE (Optional)                    │
│  ┌───────────────────────────────────────────────────────────┐  │
│  │  Apache Airflow (Port 8081)                               │  │
│  │  - Scheduled ETL jobs                                     │  │
│  │  - Streaming data pipeline                                │  │
│  │  - Bullhorn sync DAG                                      │  │
│  └───────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────┘
```

### Component Interactions

1. **API ↔ Bullhorn**: HTTP REST API with OAuth 2.0 authentication
2. **API ↔ PostgreSQL**: Prisma ORM for all database operations
3. **API ↔ ML Service**: HTTP requests for embeddings and similarity scoring
4. **Dashboard ↔ API**: React Query for data fetching, Zustand for state
5. **Airflow ↔ PostgreSQL**: Direct database access for ETL operations
6. **Airflow ↔ Bullhorn**: Scheduled data synchronization

### Data Flow

```
1. SYNC: Bullhorn → API → PostgreSQL
   - Fetch candidates, consultants, positions
   - Transform and normalize data
   - Store with embeddings

2. MATCH: PostgreSQL → ML Service → PostgreSQL
   - Generate embeddings for candidates and positions
   - Calculate similarity scores
   - Store match results

3. REFERRAL: Dashboard → API → Bullhorn
   - Consultant creates referral
   - API validates and processes
   - Submit to Bullhorn as job submission
```

---

## Technology Stack

### Backend (API)

| Technology | Version | Purpose |
|------------|---------|---------|
| **Node.js** | 20+ | Runtime environment |
| **Express.js** | 4.x | Web framework |
| **TypeScript** | 5.3+ | Type-safe development |
| **Prisma** | 5.x | ORM and database toolkit |
| **Winston** | 3.x | Logging |
| **Zod** | 3.x | Schema validation |

### Frontend (Dashboard)

| Technology | Version | Purpose |
|------------|---------|---------|
| **Next.js** | 16.x | React framework |
| **React** | 19.x | UI library |
| **Tailwind CSS** | 4.x | Styling |
| **TanStack Query** | 5.x | Data fetching |
| **Zustand** | 5.x | State management |
| **Framer Motion** | 12.x | Animations |

### ML Service

| Technology | Version | Purpose |
|------------|---------|---------|
| **Python** | 3.11+ | Runtime |
| **FastAPI** | Latest | Web framework |
| **sentence-transformers** | Latest | Embeddings model |
| **NumPy** | Latest | Numerical operations |

### Database

| Technology | Version | Purpose |
|------------|---------|---------|
| **PostgreSQL** | 15 | Primary database |
| **pgvector** | Latest | Vector similarity search |

### Data Pipeline

| Technology | Version | Purpose |
|------------|---------|---------|
| **Apache Airflow** | 2.8.x | Workflow orchestration |
| **Pandas** | Latest | Data transformation |
| **psycopg2** | Latest | PostgreSQL adapter |

### External Services

| Technology | Version | Purpose |
|------------|---------|---------|
| **Java Spring Boot** | 3.5.7 | Bullhorn mock service |
| **LocalStack** | Latest | AWS services mock |
| **Docker** | Latest | Containerization |

---

## Service Ports Reference

| Service | Port | URL | Credentials |
|---------|------|-----|-------------|
| **API** | 3001 | http://localhost:3001 | N/A |
| **Dashboard** | 3000 | http://localhost:3000 | N/A |
| **ML Service** | 8000 | http://localhost:8000 | N/A |
| **Bullhorn Mock** | 8082 | http://localhost:8082 | test-client-1 / test-secret-1 |
| **Airflow UI** | 8081 | http://localhost:8081 | admin / admin |
| **PostgreSQL (Main)** | 5433 | localhost:5433 | referral_user / referral_pass |
| **PostgreSQL (Bullhorn)** | 5434 | localhost:5434 | postgres / postgres |
| **pgAdmin (Main)** | 5051 | http://localhost:5051 | admin@admin.com / admin |
| **pgAdmin (Bullhorn)** | 5052 | http://localhost:5052 | admin@admin.com / admin |
| **Prisma Studio** | 5555 | http://localhost:5555 | Run `npm run db:studio` |
| **LocalStack** | 4566 | http://localhost:4566 | test / test |

**Note**: API runs on port 3001, Dashboard runs on port 3000.

---

## Environment Configuration

### Main Environment File (.env)

Location: `<root>/.env`

```env
# ============================================
# SERVER CONFIGURATION
# ============================================
PORT=3001
NODE_ENV=development

# ============================================
# DATABASE CONFIGURATION
# ============================================
DATABASE_URL="postgresql://referral_user:referral_pass@localhost:5433/referral_system?schema=public"

# ============================================
# REDIS CONFIGURATION (Optional - for queues)
# ============================================
REDIS_HOST=localhost
REDIS_PORT=6379
REDIS_PASSWORD=

# ============================================
# BULLHORN CONFIGURATION
# ============================================

# Mock Mode (DEFAULT - No real credentials needed!)
BULLHORN_MOCK_MODE=true

# Mock Bullhorn Service URLs
BULLHORN_MOCK_BASE_URL=http://localhost:8082
BULLHORN_MOCK_CLIENT_ID=test-client-1
BULLHORN_MOCK_CLIENT_SECRET=test-secret-1
BULLHORN_MOCK_USERNAME=admin@bullhorn.local
BULLHORN_MOCK_PASSWORD=password123

# Real Bullhorn Credentials (only when BULLHORN_MOCK_MODE=false)
BULLHORN_CLIENT_ID=your_client_id_here
BULLHORN_CLIENT_SECRET=your_client_secret_here
BULLHORN_USERNAME=your_username_here
BULLHORN_PASSWORD=your_password_here
BULLHORN_AUTH_URL=https://auth.bullhornstaffing.com/oauth/authorize
BULLHORN_TOKEN_URL=https://auth.bullhornstaffing.com/oauth/token
BULLHORN_LOGIN_URL=https://rest.bullhornstaffing.com/rest-services/login
BULLHORN_REST_URL=https://rest.bullhornstaffing.com/rest-services

# ============================================
# SYNC & MATCHING CONFIGURATION
# ============================================
SYNC_INTERVAL_MINUTES=30
MATCHING_SCORE_THRESHOLD=70

# ============================================
# ML SERVICE CONFIGURATION
# ============================================
ML_SERVICE_URL=http://localhost:8000

# ============================================
# LOGGING CONFIGURATION
# ============================================
LOG_LEVEL=info
```

### Airflow Environment File

Location: `pipelines/airflow/.env.airflow`

```env
# Airflow Core Settings
AIRFLOW__CORE__EXECUTOR=LocalExecutor
AIRFLOW__CORE__FERNET_KEY=81HqDtbqAywKSOumSha3BhWNOdQ26slT6K0YaZeZyPs=
AIRFLOW__CORE__DAGS_ARE_PAUSED_AT_CREATION=True
AIRFLOW__CORE__LOAD_EXAMPLES=False

# Airflow Database
AIRFLOW__DATABASE__SQL_ALCHEMY_CONN=postgresql+psycopg2://referral_user:referral_pass@postgres:5432/airflow

# Airflow Webserver
AIRFLOW__WEBSERVER__SECRET_KEY=referral_system_secret_key
AIRFLOW__WEBSERVER__EXPOSE_CONFIG=True

# Database connection for data pipeline
DB_HOST=postgres
DB_PORT=5432
DB_NAME=referral_system
DB_USER=referral_user
DB_PASSWORD=referral_pass

# Bullhorn Mock Service
BULLHORN_MOCK_BASE_URL=http://bullhorn-mock:8080
BULLHORN_MOCK_CLIENT_ID=test-client-1
BULLHORN_MOCK_CLIENT_SECRET=test-secret-1
BULLHORN_MOCK_USERNAME=admin@bullhorn.local
BULLHORN_MOCK_PASSWORD=password123

# ML Service
ML_SERVICE_URL=http://ml-service:8000
```

---

## Docker Services

### Docker Compose Overview

Location: `infrastructure/docker/docker-compose.yml`

This file defines all infrastructure services needed for the system.

### Services & Dependencies

#### 1. PostgreSQL (Main Application)

```yaml
Service: postgres
Container: referral-system-db
Image: pgvector/pgvector:pg15
Port: 5433 → 5432
Database: referral_system
User: referral_user
Password: referral_pass
```

**Purpose**: Main application database with pgvector extension for embeddings.

#### 2. PostgreSQL (Bullhorn Mock)

```yaml
Service: bullhorn-postgres
Container: bullhorn-postgres
Image: postgres:15
Port: 5434 → 5432
Database: bullhorn_mock_dev
User: postgres
Password: postgres
```

**Purpose**: Separate database for Bullhorn mock service.

#### 3. Bullhorn Mock Service

```yaml
Service: bullhorn-mock
Container: bullhorn-mock
Built From: services/bullhorn-mock/Dockerfile
Port: 8082 → 8080
Depends On: bullhorn-postgres (healthy)
Health Check: /actuator/health
```

**Purpose**: Simulates Bullhorn CRM API for development/testing.

#### 4. ML Service (Embedding)

```yaml
Service: ml-service
Container: referral-system-ml
Built From: apps/embedding-service/Dockerfile
Port: 8000 → 8000
Health Check: /health
```

**Purpose**: Generates embeddings and similarity scores using sentence-transformers.

#### 5. Airflow Webserver

```yaml
Service: airflow-webserver
Container: referral-system-airflow-webserver
Built From: pipelines/airflow/Dockerfile
Port: 8081 → 8080
Depends On: postgres
Env File: pipelines/airflow/.env.airflow
```

**Purpose**: Airflow UI for managing DAGs and monitoring pipelines.

#### 6. Airflow Scheduler

```yaml
Service: airflow-scheduler
Container: referral-system-airflow-scheduler
Built From: pipelines/airflow/Dockerfile
No External Port
Depends On: postgres
```

**Purpose**: Executes scheduled Airflow tasks.

#### 7. pgAdmin (Main)

```yaml
Service: pgadmin
Container: referral-system-pgadmin
Image: dpage/pgadmin4:latest
Port: 5051 → 80
Login: admin@admin.com / admin
```

**Purpose**: Web-based PostgreSQL administration tool.

#### 8. pgAdmin (Bullhorn)

```yaml
Service: bullhorn-pgadmin
Container: bullhorn-pgadmin
Image: dpage/pgadmin4:latest
Port: 5052 → 80
Login: admin@admin.com / admin
```

**Purpose**: Separate pgAdmin for Bullhorn database.

### Starting & Managing Services

```bash
# Start all services
npm run infra:up
# or
docker-compose -f infrastructure/docker/docker-compose.yml up -d

# Stop all services
npm run infra:down

# View logs
npm run infra:logs
# or specific service
docker-compose -f infrastructure/docker/docker-compose.yml logs -f postgres

# Check status
docker-compose -f infrastructure/docker/docker-compose.yml ps

# Restart a service
docker-compose -f infrastructure/docker/docker-compose.yml restart bullhorn-mock

# Rebuild a service
docker-compose -f infrastructure/docker/docker-compose.yml up -d --build ml-service
```

### Service Startup Order

The docker-compose file handles dependencies automatically:

1. **PostgreSQL databases** (main & bullhorn) start first
2. **Bullhorn Mock** starts after bullhorn-postgres is healthy
3. **ML Service** starts independently
4. **Airflow** services start after main postgres is ready
5. **pgAdmin** instances start after their respective databases
6. **LocalStack** starts independently

Wait ~60 seconds after `npm run infra:up` for all health checks to pass.

---

## Database Setup

### Schema Overview

Location: `packages/database/prisma/schema.prisma`

#### Core Models

**Consultant**
- Recruitment consultants/agents
- Links to Bullhorn user records
- Tracks specialization and regions

**Candidate**
- Job seekers from Bullhorn
- Stores skills, experience, embeddings
- Includes resume parsing data

**Position**
- Open job orders from Bullhorn
- Requirements and compensation details
- Embeddings for semantic matching

**Match**
- AI-generated candidate-position pairs
- Similarity scores and explanations
- Status tracking (pending, reviewed, accepted, rejected)

**Referral**
- Referrals submitted to Bullhorn
- Links to matches and consultants
- Submission status tracking

**CandidateConnection**
- Network graph of candidate relationships
- Connection types and strengths
- For network-based recommendations

**ConsultantActivity**
- Tracks consultant actions
- Calls, emails, meetings
- Activity analytics

**Job**
- Background job tracking
- Status and progress monitoring
- Error handling

**PipelineRun**
- ETL pipeline execution history
- Records processed, errors
- Performance metrics

### Database Commands

```bash
# Generate Prisma Client (required after schema changes)
npm run db:generate

# Create and apply migrations
npm run db:migrate
# Enter migration name when prompted

# Push schema without migration (dev only)
npm run db:push

# Reset database (WARNING: deletes all data)
npm run db:push -- --force-reset

# Seed test data
npm run db:seed

# Open Prisma Studio (visual database browser)
npm run db:studio
# Opens at http://localhost:5555

# Clean up all data
npm run db:cleanup
```

### Seed Data

The seed script (`packages/database/prisma/seed.ts`) creates:

**3 Consultants:**
- John Smith (Senior Recruiter, Tech specialization)
- Sarah Johnson (Placement Specialist, Healthcare)
- Additional consultant

**3+ Candidates:**
- Alice Williams (Senior Developer, 8 years)
- Bob Davis (Product Manager, 6 years)
- Carol Martinez (DevOps Engineer, 5 years)
- Additional candidates

**3+ Positions:**
- Senior Full Stack Engineer ($120k-$160k)
- Product Manager ($130k-$170k)
- DevOps Engineer ($110k-$150k)
- Additional positions

### Database Migrations

Location: `packages/database/prisma/migrations/`

**Creating a new migration:**
```bash
# Make changes to schema.prisma
# Then create migration
npm run db:migrate

# Enter descriptive name (e.g., "add_candidate_connections")
```

**Applying migrations:**
```bash
# Apply all pending migrations
npm run db:migrate
```

### Direct Database Access

**Using Prisma Studio (Recommended):**
```bash
npm run db:studio
```

**Using pgAdmin:**
1. Open http://localhost:5051
2. Login: admin@admin.com / admin
3. Add server:
   - Host: host.docker.internal (or localhost)
   - Port: 5433
   - Database: referral_system
   - Username: referral_user
   - Password: referral_pass

**Using psql:**
```bash
docker exec -it referral-system-db psql -U referral_user -d referral_system
```

---

## NPM Scripts Reference

### Root-Level Scripts

Location: `<root>/package.json`

#### Development

```bash
# Start all apps in development mode
npm run dev

# Start infrastructure + all apps
npm run dev:full
```

#### Build & Production

```bash
# Build all apps and packages
npm run build

# Start production builds
npm run start

# Run linting
npm run lint

# Run tests
npm run test
```

#### Infrastructure Management

```bash
# Start all Docker services
npm run infra:up

# Stop all Docker services
npm run infra:down

# View Docker logs (follow mode)
npm run infra:logs
```

#### Database Operations

```bash
# Generate Prisma client
npm run db:generate

# Run database migrations
npm run db:migrate

# Push schema to database (no migrations)
npm run db:push

# Seed test data
npm run db:seed

# Open Prisma Studio GUI
npm run db:studio

# Clean up all database data
npm run db:cleanup
```

### App-Specific Scripts

#### API Server

```bash
# Start API in development
npm run dev --filter=@referral-system/api

# Build API
npm run build --filter=@referral-system/api

# Start production API
npm run start --filter=@referral-system/api
```

#### Consultant Dashboard

```bash
# Start dashboard in development
npm run dev --filter=consultant-dashboard

# Build dashboard
npm run build --filter=consultant-dashboard

# Start production dashboard
npm run start --filter=consultant-dashboard
```

#### Database Package

```bash
# Generate Prisma client
npm run db:generate --filter=@referral-system/database

# Run migrations
npm run db:migrate --filter=@referral-system/database

# Open Prisma Studio
npm run db:studio --filter=@referral-system/database
```

### Turborepo Features

**Run command for specific workspace:**
```bash
npm run <command> --filter=<package-name>
```

**Run with dependencies:**
```bash
npm run build --filter=@referral-system/api...
```

**Parallel execution:**
Turborepo automatically parallelizes independent tasks for faster builds.

**Caching:**
Turborepo caches task outputs. Clear cache with:
```bash
rm -rf .turbo
```

---

## Development Workflow

### Daily Development Routine

**Terminal 1: Start Everything**
```bash
cd referral-system

# Start all services
npm run dev

# This starts:
# - API server (http://localhost:3001)
# - Dashboard (http://localhost:3000)
# - Watches for file changes
```

**Terminal 2 (Optional): Database GUI**
```bash
npm run db:studio

# Opens Prisma Studio at http://localhost:5555
# Visual interface for viewing/editing data
```

**Terminal 3 (Optional): Docker Logs**
```bash
npm run infra:logs

# Monitor all Docker service logs in real-time
```

### Making Changes

#### API Changes

1. Edit files in `apps/api/src/`
2. Server auto-restarts on save
3. Test with curl or Postman
4. Check logs in terminal

#### Database Changes

1. Edit `packages/database/prisma/schema.prisma`
2. Generate client: `npm run db:generate`
3. Create migration: `npm run db:migrate`
4. Server auto-restarts with new schema

#### Dashboard Changes

1. Edit files in `apps/consultant-dashboard/`
2. Hot reload updates browser automatically
3. Check browser console for errors

### Testing Workflow

#### 1. Manual API Testing

```bash
# Health check
curl http://localhost:3001/api/dashboard/health

# Sync data from Bullhorn
curl -X POST http://localhost:3001/api/sync/all

# Check stats
curl http://localhost:3001/api/dashboard/stats

# Get matches
curl http://localhost:3001/api/matches?minScore=70
```

#### 2. Using Postman/Insomnia

Import collection from `docs/api/` (if available) or create requests manually.

#### 3. Database Verification

```bash
# Open Prisma Studio
npm run db:studio

# Check data was synced/created correctly
```

### Git Workflow

```bash
# Create feature branch
git checkout -b feature/your-feature-name

# Make changes and commit
git add .
git commit -m "feat: description of changes"

# Push to remote
git push origin feature/your-feature-name

# Create pull request
```

### Common Development Tasks

**Add a new API endpoint:**
1. Add route in `apps/api/src/routes/`
2. Add controller in `apps/api/src/controllers/`
3. Add service logic in `apps/api/src/services/`
4. Test with curl

**Add a new database model:**
1. Edit `packages/database/prisma/schema.prisma`
2. Run `npm run db:generate`
3. Run `npm run db:migrate`
4. Update TypeScript types if needed

**Add a new page to dashboard:**
1. Create component in `apps/consultant-dashboard/src/app/`
2. Add routing if needed
3. Test in browser

**Debug issues:**
1. Check terminal logs
2. Check browser console (dashboard)
3. Check Docker logs: `docker-compose logs <service>`
4. Use Prisma Studio to inspect database

---

## API Endpoints

Base URL: `http://localhost:3001/api`

### Health & Monitoring

```http
GET /api/dashboard/health
```
Check database and Redis connectivity.

**Response:**
```json
{
  "status": "ok",
  "database": "connected",
  "redis": "connected"
}
```

```http
GET /api/dashboard/stats
```
Get system statistics.

**Response:**
```json
{
  "consultants": 3,
  "candidates": 15,
  "positions": 8,
  "matches": 45,
  "referrals": 12,
  "queueStatus": {
    "active": 0,
    "waiting": 0,
    "completed": 25,
    "failed": 1
  }
}
```

### Sync Operations

```http
POST /api/sync/all
```
Sync all data from Bullhorn (consultants, candidates, positions).

```http
POST /api/sync/consultants
```
Sync only consultants.

```http
POST /api/sync/candidates
```
Sync only candidates.

```http
POST /api/sync/positions
```
Sync only positions.

### Matches

```http
GET /api/matches
```
List all matches with filtering.

**Query Parameters:**
- `minScore` - Minimum match score (0-100)
- `positionId` - Filter by position
- `candidateId` - Filter by candidate
- `status` - Filter by status (pending, reviewed, accepted, rejected)

```http
GET /api/matches/:id
```
Get match details.

```http
GET /api/matches/positions/:positionId/similar-candidates
```
Semantic search for similar candidates.

**Query Parameters:**
- `limit` - Number of results (default: 10)
- `minScore` - Minimum similarity score

```http
PATCH /api/matches/:id
```
Update match status.

**Body:**
```json
{
  "status": "accepted",
  "notes": "Great fit for the role"
}
```

### Referrals

```http
GET /api/referrals
```
List all referrals.

**Query Parameters:**
- `status` - Filter by status
- `consultantId` - Filter by consultant

```http
GET /api/referrals/:id
```
Get referral details.

```http
POST /api/referrals
```
Create new referral.

**Body:**
```json
{
  "matchId": "uuid",
  "consultantId": "uuid",
  "referralSource": "LinkedIn",
  "referrerName": "John Doe",
  "notes": "Strong technical background"
}
```

```http
POST /api/referrals/auto-create
```
Auto-create referrals for high-scoring matches.

**Query Parameters:**
- `minScore` - Minimum score threshold (default: 85)

```http
POST /api/referrals/:id/send
```
Send referral to Bullhorn.

```http
POST /api/referrals/send-pending
```
Send all pending referrals to Bullhorn.

```http
PATCH /api/referrals/:id
```
Update referral status.

### Typical API Workflow

```bash
# 1. Sync data from Bullhorn
curl -X POST http://localhost:3001/api/sync/all

# 2. Wait ~30 seconds for background jobs

# 3. Check sync results
curl http://localhost:3001/api/dashboard/stats

# 4. View top matches
curl "http://localhost:3001/api/matches?minScore=75"

# 5. Create referral from a match
curl -X POST http://localhost:3001/api/referrals \
  -H "Content-Type: application/json" \
  -d '{
    "matchId": "match-uuid-here",
    "consultantId": "consultant-uuid-here",
    "referralSource": "Internal Database"
  }'

# 6. Send referral to Bullhorn
curl -X POST http://localhost:3001/api/referrals/{referral-id}/send
```

---

## Testing the System

### End-to-End Test Scenario

#### Step 1: Verify Infrastructure

```bash
# Check all Docker services are running
docker-compose -f infrastructure/docker/docker-compose.yml ps

# All services should show "Up" and "(healthy)"
```

#### Step 2: Check API Health

```bash
curl http://localhost:3001/api/dashboard/health

# Expected: {"status":"ok","database":"connected"}
```

#### Step 3: Sync Data from Bullhorn

```bash
# Trigger sync of all data
curl -X POST http://localhost:3001/api/sync/all

# Response indicates jobs were queued
```

#### Step 4: Wait for Background Jobs

```bash
# Wait ~30-60 seconds for sync to complete
sleep 30

# Check system stats
curl http://localhost:3001/api/dashboard/stats

# Should show counts for consultants, candidates, positions
```

#### Step 5: View Generated Matches

```bash
# Get matches with score >= 70
curl "http://localhost:3001/api/matches?minScore=70" | jq

# Examine match scores and reasoning
```

#### Step 6: Test Semantic Search

```bash
# Find similar candidates for a position
curl "http://localhost:3001/api/matches/positions/{position-id}/similar-candidates?limit=5" | jq

# Shows candidates ranked by embedding similarity
```

#### Step 7: Create a Referral

```bash
# Create referral from a match
curl -X POST http://localhost:3001/api/referrals \
  -H "Content-Type: application/json" \
  -d '{
    "matchId": "<match-uuid>",
    "consultantId": "<consultant-uuid>",
    "referralSource": "Database Search",
    "notes": "Strong technical fit"
  }' | jq

# Returns created referral with ID
```

#### Step 8: Send Referral to Bullhorn

```bash
# Send referral
curl -X POST http://localhost:3001/api/referrals/{referral-id}/send | jq

# Response includes Bullhorn submission ID
```

#### Step 9: Verify in Bullhorn Mock

```bash
# Check submission was created
curl http://localhost:8082/api/v1/submissions | jq

# Should show your new submission
```

### Testing Airflow Pipeline

#### Access Airflow UI

1. Open http://localhost:8081
2. Login: admin / admin
3. Find "bullhorn_sync_dag"

#### Trigger Manual Run

1. Click on "bullhorn_sync_dag"
2. Click "Trigger DAG" button (play icon)
3. Monitor progress in Graph view

#### Check DAG Logs

1. Click on a task in the Graph view
2. Click "Log" button
3. Review execution logs

### Using Prisma Studio

```bash
# Open Prisma Studio
npm run db:studio
```

1. Opens at http://localhost:5555
2. Browse all tables visually
3. Edit records directly
4. View relationships

### Using pgAdmin

1. Open http://localhost:5051
2. Login: admin@admin.com / admin
3. Add server:
   - Name: Referral System
   - Host: host.docker.internal
   - Port: 5433
   - Username: referral_user
   - Password: referral_pass
4. Run SQL queries in Query Tool

---

## Key Files & Directories

```
referral-system/
├── .env                                   # Environment variables (create from .env.example)
├── .env.example                           # Environment template
├── package.json                           # Root package with workspace scripts
├── turbo.json                             # Turborepo configuration
├── README.md                              # Main project documentation
├── DEVELOPER_ONBOARDING.md               # This file
│
├── apps/                                  # Applications
│   ├── api/                              # Express API server
│   │   ├── src/
│   │   │   ├── index.ts                  # Entry point
│   │   │   ├── app.ts                    # Express app setup
│   │   │   ├── config/                   # Configuration
│   │   │   │   ├── database.ts           # Prisma client setup
│   │   │   │   ├── logger.ts             # Winston logger
│   │   │   │   └── env.ts                # Environment validation
│   │   │   ├── routes/                   # API routes
│   │   │   │   ├── index.ts              # Route registration
│   │   │   │   ├── dashboard.routes.ts   # Dashboard endpoints
│   │   │   │   ├── sync.routes.ts        # Sync endpoints
│   │   │   │   ├── matches.routes.ts     # Match endpoints
│   │   │   │   └── referrals.routes.ts   # Referral endpoints
│   │   │   ├── controllers/              # Route handlers
│   │   │   ├── services/                 # Business logic
│   │   │   │   ├── sync.service.ts       # Bullhorn sync
│   │   │   │   ├── matching.service.ts   # ML matching
│   │   │   │   └── referral.service.ts   # Referral management
│   │   │   ├── middleware/               # Express middleware
│   │   │   └── utils/                    # Utilities
│   │   └── package.json
│   │
│   ├── consultant-dashboard/             # Next.js frontend
│   │   ├── src/
│   │   │   ├── app/                      # Next.js app router pages
│   │   │   ├── components/               # React components
│   │   │   ├── hooks/                    # Custom React hooks
│   │   │   └── lib/                      # Utilities
│   │   └── package.json
│   │
│   └── embedding-service/                # FastAPI ML service
│       ├── main.py                       # Entry point
│       ├── requirements.txt              # Python dependencies
│       └── Dockerfile                    # Container build
│
├── packages/                             # Shared packages
│   ├── database/                         # Prisma ORM package
│   │   ├── prisma/
│   │   │   ├── schema.prisma            # **Database schema**
│   │   │   ├── seed.ts                  # Seed data script
│   │   │   └── migrations/              # Database migrations
│   │   ├── src/
│   │   │   └── index.ts                 # Export Prisma client
│   │   └── package.json
│   │
│   ├── shared/                          # Shared utilities
│   │   ├── src/
│   │   │   ├── types/                   # TypeScript types
│   │   │   ├── constants/               # Constants
│   │   │   └── utils/                   # Utility functions
│   │   └── package.json
│   │
│   └── bullhorn-client/                 # Bullhorn API client
│       ├── src/
│       │   ├── client.ts                # Main client
│       │   └── types.ts                 # API types
│       └── package.json
│
├── services/                            # External services
│   └── bullhorn-mock/                   # Java Spring Boot mock
│       ├── src/
│       │   └── main/
│       │       └── java/
│       │           └── com/bullhorn/mock/
│       ├── pom.xml                      # Maven config
│       └── Dockerfile
│
├── pipelines/                           # Data pipelines
│   ├── airflow/                         # Apache Airflow
│   │   ├── dags/                        # DAG definitions
│   │   │   └── bullhorn_sync_dag.py     # Main sync DAG
│   │   ├── logs/                        # Airflow logs
│   │   ├── plugins/                     # Custom plugins
│   │   ├── .env.airflow                 # Airflow config
│   │   └── Dockerfile
│   │
│   └── data_platform/                   # Python ETL modules
│       ├── crms/
│       │   └── bullhorn/                # Bullhorn integration
│       │       ├── extractor.py         # Data extraction
│       │       └── transformer.py       # Data transformation
│       ├── loaders/
│       │   └── database_loader.py       # Bulk database loader
│       ├── validators/                  # Data quality checks
│       └── requirements.txt             # Python dependencies
│
├── infrastructure/                      # Infrastructure config
│   └── docker/
│       └── docker-compose.yml          # **All services definition**
│
├── docs/                                # Documentation
│   └── guides/                          # Additional guides
│
├── scripts/                             # Utility scripts
│   ├── cleanup-all-data.ts             # Database cleanup
│   └── sync-performance-comparison.ts   # Performance testing
│
└── logs/                                # Application logs
    ├── combined.log                     # All logs
    └── error.log                        # Error logs only
```

### Important Files to Know

| File | Purpose |
|------|---------|
| `packages/database/prisma/schema.prisma` | Database schema definition |
| `infrastructure/docker/docker-compose.yml` | All Docker services |
| `.env` | Environment configuration |
| `apps/api/src/index.ts` | API entry point |
| `apps/api/src/routes/` | All API endpoints |
| `pipelines/airflow/dags/bullhorn_sync_dag.py` | ETL pipeline definition |
| `turbo.json` | Turborepo task configuration |

---

## Troubleshooting

### Common Issues & Solutions

#### Port Already in Use

**Error**: `EADDRINUSE: address already in use :::3001`

**Solution**:
```bash
# Find process using port
lsof -i :3001

# Kill the process
kill -9 <PID>

# Or change port in .env
PORT=3001
```

#### Database Connection Failed

**Error**: `Cannot connect to database`

**Solution**:
```bash
# Check PostgreSQL is running
docker ps | grep postgres

# If not running, start infrastructure
npm run infra:up

# Wait for health check
sleep 30

# Verify connection
docker exec referral-system-db pg_isready -U referral_user
```

#### Prisma Client Not Generated

**Error**: `Cannot find module '@prisma/client'`

**Solution**:
```bash
# Generate Prisma client
npm run db:generate

# Restart dev server
npm run dev
```

#### Migration Failed

**Error**: Migration errors or schema conflicts

**Solution**:
```bash
# Option 1: Push schema without migration (dev only)
npm run db:push

# Option 2: Reset database (WARNING: deletes data)
npm run db:push -- --force-reset

# Option 3: Delete migration and retry
rm -rf packages/database/prisma/migrations/<migration-name>
npm run db:migrate
```

#### Bullhorn Mock Not Responding

**Error**: `Connection refused to localhost:8082`

**Solution**:
```bash
# Check bullhorn-postgres is healthy first
docker ps | grep bullhorn-postgres

# Check bullhorn-mock logs
docker logs bullhorn-mock

# Restart if needed
docker-compose -f infrastructure/docker/docker-compose.yml restart bullhorn-mock

# Wait for health check
sleep 30
```

#### ML Service Errors

**Error**: Model loading failures

**Solution**:
```bash
# Check ML service logs
docker logs referral-system-ml

# Rebuild container (downloads model)
docker-compose -f infrastructure/docker/docker-compose.yml up -d --build ml-service

# First run takes ~1-2 minutes to download model
```

#### Airflow Database Not Initialized

**Error**: `database "airflow" does not exist`

**Solution**:
```bash
# Create airflow database
docker exec referral-system-db psql -U referral_user -d postgres -c "CREATE DATABASE airflow;"

# Initialize Airflow
docker run --rm --network docker_default \
  --env-file pipelines/airflow/.env.airflow \
  apache/airflow:2.8.1 airflow db init

# Create admin user
docker run --rm --network docker_default \
  --env-file pipelines/airflow/.env.airflow \
  apache/airflow:2.8.1 \
  airflow users create --username admin --password admin \
  --firstname Admin --lastname User --role Admin \
  --email admin@example.com

# Restart Airflow
docker-compose -f infrastructure/docker/docker-compose.yml restart airflow-webserver airflow-scheduler
```

#### Turbo Cache Issues

**Error**: Stale builds or cached errors

**Solution**:
```bash
# Clear Turbo cache
rm -rf .turbo

# Clear node_modules and reinstall
rm -rf node_modules
npm install

# Rebuild
npm run build
```

#### Environment Variables Not Loaded

**Error**: `undefined` values for environment variables

**Solution**:
```bash
# Ensure .env exists
ls -la .env

# If not, copy from template
cp .env.example .env

# Edit values
nano .env

# Restart services
npm run dev
```

#### Docker Out of Memory

**Error**: Containers crashing or OOM errors

**Solution**:
1. Open Docker Desktop
2. Go to Settings → Resources
3. Increase Memory to at least 4GB (8GB recommended)
4. Restart Docker
5. Run `npm run infra:up` again

#### pgvector Extension Not Found

**Error**: `extension "vector" does not exist`

**Solution**:
```bash
# Ensure using pgvector image
docker-compose -f infrastructure/docker/docker-compose.yml down
docker volume rm docker_postgres_data

# Restart with correct image (pgvector/pgvector:pg15)
npm run infra:up
npm run db:migrate
```

### Getting Help

1. **Check logs**: `npm run infra:logs`
2. **Check Docker status**: `docker ps`
3. **Review environment**: Ensure `.env` is configured
4. **Check database**: Use Prisma Studio or pgAdmin
5. **Restart services**: `npm run infra:down && npm run infra:up`

---

## Monitoring & Debugging

### Application Logs

**Location**: `logs/` directory

```bash
# View all logs
tail -f logs/combined.log

# View error logs only
tail -f logs/error.log

# Search logs
grep "ERROR" logs/combined.log
```

### Docker Logs

```bash
# All services
npm run infra:logs

# Specific service
docker logs referral-system-db
docker logs bullhorn-mock
docker logs referral-system-ml
docker logs referral-system-airflow-webserver

# Follow mode
docker logs -f bullhorn-mock

# Last 100 lines
docker logs --tail 100 referral-system-db
```

### Database Monitoring

#### Prisma Studio (Recommended)

```bash
npm run db:studio
# Opens at http://localhost:5555
```

Features:
- Visual data browser
- Edit records inline
- View relationships
- Filter and search

#### pgAdmin

1. Open http://localhost:5051
2. Login: admin@admin.com / admin
3. Connect to server (host.docker.internal:5433)
4. Run queries, view stats, manage data

#### Direct psql

```bash
# Main database
docker exec -it referral-system-db psql -U referral_user -d referral_system

# Bullhorn database
docker exec -it bullhorn-postgres psql -U postgres -d bullhorn_mock_dev

# Common queries
\dt                    # List tables
\d table_name         # Describe table
SELECT COUNT(*) FROM "Candidate";
```

### API Debugging

#### Health Checks

```bash
# API health
curl http://localhost:3001/api/dashboard/health

# Bullhorn mock health
curl http://localhost:8082/actuator/health

# ML service health
curl http://localhost:8000/health
```

#### Request Logging

The API logs all requests with Winston. Check `logs/combined.log`.

#### Response Inspection

```bash
# Verbose curl
curl -v http://localhost:3001/api/dashboard/stats

# Pretty JSON with jq
curl http://localhost:3001/api/matches | jq
```

### Performance Monitoring

#### Database Query Performance

```sql
-- In psql, enable timing
\timing

-- Run query
SELECT * FROM "Match" WHERE score > 70;

-- View query plan
EXPLAIN ANALYZE SELECT * FROM "Match" WHERE score > 70;
```

#### API Response Times

Check API logs for response times:
```bash
grep "Response time" logs/combined.log
```

### Airflow Monitoring

1. **Airflow UI**: http://localhost:8081
   - View DAG runs
   - Check task logs
   - Monitor execution times

2. **Database**: Check `PipelineRun` table in Prisma Studio

3. **Logs**: `pipelines/airflow/logs/`

### Docker Resource Usage

```bash
# Container stats
docker stats

# Disk usage
docker system df

# Clean up unused resources
docker system prune
```

---

## Switching to Real Bullhorn

By default, the system uses the **Bullhorn Mock Service** for development. When you have real Bullhorn credentials, follow these steps:

### Step 1: Obtain Bullhorn Credentials

Contact your Bullhorn administrator to get:
- Client ID
- Client Secret
- Username
- Password
- API URLs (if different from defaults)

### Step 2: Update Environment Variables

Edit `.env`:

```env
# Change mock mode to false
BULLHORN_MOCK_MODE=false

# Add real credentials
BULLHORN_CLIENT_ID=your_real_client_id
BULLHORN_CLIENT_SECRET=your_real_client_secret
BULLHORN_USERNAME=your_username@company.com
BULLHORN_PASSWORD=your_secure_password

# Use real Bullhorn URLs
BULLHORN_AUTH_URL=https://auth.bullhornstaffing.com/oauth/authorize
BULLHORN_TOKEN_URL=https://auth.bullhornstaffing.com/oauth/token
BULLHORN_LOGIN_URL=https://rest.bullhornstaffing.com/rest-services/login
BULLHORN_REST_URL=https://rest.bullhornstaffing.com/rest-services
```

### Step 3: Restart API

```bash
# Stop current API
# (Ctrl+C if running in terminal)

# Start API with new config
npm run dev --filter=@referral-system/api

# Check logs for: "Using real Bullhorn Service"
```

### Step 4: Test Connection

```bash
# Trigger sync with real Bullhorn
curl -X POST http://localhost:3001/api/sync/all

# Check logs for authentication success
tail -f logs/combined.log | grep Bullhorn

# Verify data in database
npm run db:studio
```

### Step 5: Update Airflow (if using)

Edit `pipelines/airflow/.env.airflow`:

```env
# Update to use real Bullhorn
BULLHORN_CLIENT_ID=your_real_client_id
BULLHORN_CLIENT_SECRET=your_real_client_secret
BULLHORN_USERNAME=your_username@company.com
BULLHORN_PASSWORD=your_secure_password
BULLHORN_BASE_URL=https://rest.bullhornstaffing.com/rest-services
```

Restart Airflow:
```bash
docker-compose -f infrastructure/docker/docker-compose.yml restart airflow-webserver airflow-scheduler
```

### Differences Between Mock and Real

| Aspect | Mock Service | Real Bullhorn |
|--------|--------------|---------------|
| **Authentication** | Simple test credentials | OAuth 2.0 with tokens |
| **Data** | Pre-generated test data | Your actual production data |
| **Rate Limits** | None | Bullhorn API rate limits apply |
| **Response Times** | ~200-700ms artificial delay | Variable based on Bullhorn |
| **IDs** | All IDs > 1000 | Real entity IDs |
| **Webhooks** | Not supported | Full webhook support |

### Troubleshooting Real Bullhorn

**Authentication fails:**
- Verify credentials are correct
- Check if account has API access enabled
- Ensure IP whitelisting (if required)

**Rate limit errors:**
- Reduce `SYNC_INTERVAL_MINUTES` in .env
- Implement request throttling

**Missing data:**
- Verify user permissions in Bullhorn
- Check entity access rights
- Review API field security settings

---

## Next Steps

### After Setup

1. **Explore the Dashboard**
   - Open http://localhost:3000
   - View consultants, candidates, positions
   - Review generated matches

2. **Test the Full Workflow**
   - Sync data: `POST /api/sync/all`
   - View matches: `GET /api/matches`
   - Create referral: `POST /api/referrals`
   - Send to Bullhorn: `POST /api/referrals/:id/send`

3. **Review Code Architecture**
   - Examine `apps/api/src/` structure
   - Understand service layer pattern
   - Review Prisma schema

4. **Set Up Your IDE**
   - Install ESLint and Prettier extensions
   - Configure TypeScript support
   - Set up debugging configuration

5. **Read Additional Documentation**
   - API endpoint details in code comments
   - Database schema relationships
   - ML service documentation

### Learning Resources

**Prisma ORM:**
- [Prisma Docs](https://www.prisma.io/docs)
- Schema definition guide
- Query API reference

**Next.js 16:**
- [Next.js Documentation](https://nextjs.org/docs)
- App Router guide
- Server Components

**Apache Airflow:**
- [Airflow Docs](https://airflow.apache.org/docs)
- DAG authoring guide
- Operator reference

**Bullhorn API:**
- [Bullhorn API Docs](https://bullhorn.github.io/rest-api-docs/)
- Authentication guide
- Entity reference

### Contributing

1. Follow existing code structure and patterns
2. Add tests for new features
3. Update documentation
4. Use conventional commits format
5. Request code review before merging

---

## Summary Checklist

**First Time Setup:**
- [ ] Install prerequisites (Node.js, Docker, Git)
- [ ] Clone repository
- [ ] Run `npm install`
- [ ] Copy `.env.example` to `.env`
- [ ] Run `npm run infra:up`
- [ ] Wait 60 seconds for containers
- [ ] Run `npm run db:generate`
- [ ] Run `npm run db:migrate`
- [ ] Run `npm run db:seed`
- [ ] Run `npm run dev`
- [ ] Verify health: `curl http://localhost:3001/api/dashboard/health`

**Daily Development:**
- [ ] Start Docker services: `npm run infra:up` (if not running)
- [ ] Start dev servers: `npm run dev`
- [ ] Open Prisma Studio: `npm run db:studio` (optional)
- [ ] Monitor logs: `npm run infra:logs` (optional)

**Before Committing:**
- [ ] Run linter: `npm run lint`
- [ ] Run tests: `npm run test`
- [ ] Test API endpoints manually
- [ ] Check database migrations are applied
- [ ] Update documentation if needed

---

## Quick Reference Card

```bash
# Setup
npm install                              # Install dependencies
npm run infra:up                         # Start Docker services
npm run db:generate && npm run db:migrate && npm run db:seed  # Setup database

# Development
npm run dev                              # Start all apps
npm run dev --filter=@referral-system/api  # Start API only
npm run db:studio                        # Open database GUI

# Infrastructure
npm run infra:up                         # Start Docker
npm run infra:down                       # Stop Docker
npm run infra:logs                       # View logs

# Database
npm run db:generate                      # Generate Prisma client
npm run db:migrate                       # Run migrations
npm run db:push                          # Push schema (no migration)
npm run db:seed                          # Seed test data

# Testing
curl http://localhost:3001/api/dashboard/health  # API health
curl -X POST http://localhost:3001/api/sync/all  # Sync data
curl http://localhost:3001/api/matches           # View matches

# URLs
http://localhost:3001        # API
http://localhost:3000        # Dashboard
http://localhost:8081        # Airflow (admin/admin)
http://localhost:5051        # pgAdmin (admin@admin.com/admin)
http://localhost:5555        # Prisma Studio
http://localhost:8082        # Bullhorn Mock
```

---

**Welcome to the Referral System!** If you have questions or run into issues, check the troubleshooting section or review the logs. Happy coding! 
