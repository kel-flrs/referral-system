# Referral System (Still in progress...)

AI-powered candidate-to-job matching with Bullhorn CRM integration using ML embeddings and semantic search.

**What it does:** Automatically syncs candidates and positions, generates ML embeddings, matches them using hybrid scoring, and creates referrals back to Bullhorn.

## Tech Stack

| Layer | Technologies |
|-------|-------------|
| **Frontend** | Next.js 16, React 19, Tailwind CSS, TanStack Query |
| **Backend** | Node.js 20, Express, TypeScript, Prisma |
| **ML/AI** | Python 3.11, FastAPI, sentence-transformers, pgvector |
| **Data** | PostgreSQL 15 + pgvector, Apache Airflow 2.8 |
| **Services** | Java 17, Spring Boot (Bullhorn mock) |
| **Infra** | Docker, Docker Compose |

## Quick Start

```bash
# 1. Install dependencies
npm install

# 2. Start everything (auto-initializes databases, Airflow, migrations)
npm run dev
```

**Access the applications:**
- Dashboard: http://localhost:3000
- API: http://localhost:3222
- ML Service: http://localhost:8000/docs
- Airflow: http://localhost:8081 (admin/admin)

That's it! The system automatically handles database setup, migrations, and service initialization.

## Project Structure

```
referral-system/
├── apps/
│   ├── api/              # REST API (TypeScript/Express)
│   └── web-consultant/   # Frontend (Next.js)
├── services/
│   ├── ml-service/       # ML matching service (Python/FastAPI)
│   ├── etl-service/      # ETL service
│   └── bullhorn-mock/    # Bullhorn API mock (Java/Spring)
├── packages/
│   ├── database/         # Prisma schema & client
│   ├── shared/           # Shared types & utilities
│   └── bullhorn-client/  # Bullhorn API client
├── pipelines/
│   ├── airflow/          # Airflow DAGs & orchestration
│   └── data_platform/    # Python ETL modules
└── infrastructure/       # Docker configs
```

## Architecture

```
Bullhorn CRM
    ↓
ETL Pipeline (Airflow) → Sync candidates & positions
    ↓
ML Service → Generate embeddings (sentence-transformers)
    ↓
PostgreSQL + pgvector → Store data with semantic vectors
    ↓
ML Service → Calculate matches (hybrid scoring)
    ↓
API → Serve matches to frontend
    ↓
Dashboard → Display results, create referrals
    ↓
Bullhorn CRM ← Send referrals back
```

## Development

**Common commands:**
```bash
npm run dev              # Start all services
npm run infra:down       # Stop all services
npm run db:studio        # Open database GUI
```

**See logs:**
```bash
docker logs referral-system-api              # API logs
docker logs referral-system-airflow-scheduler # Airflow logs
```

## Troubleshooting

**If something isn't working, reset everything:**
```bash
npm run infra:down
docker volume rm docker_postgres_data
npm run dev
```

**Check what's running:**
```bash
docker ps    # See all containers and their status
```
