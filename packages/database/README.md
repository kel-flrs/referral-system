# Database Package

Centralized database schema and Prisma client for the Referral System.

## Purpose

Provides a single source of truth for:
- **Database schema** definition (Prisma schema)
- **Type-safe database client** (Prisma Client)
- **Database migrations**
- **Seeding scripts**

## Tech Stack

- **ORM**: Prisma
- **Database**: PostgreSQL 15+ with pgvector extension
- **Language**: TypeScript

## Schema Overview

### Core Models

- **Candidate** - Job seekers/consultants
- **Position** - Open job positions
- **Match** - Candidate-position matches with scores
- **User** - System users (future)

### Key Features

- **Vector embeddings** using pgvector (384 dimensions)
- **Full-text search** support
- **Audit timestamps** (createdAt, updatedAt)
- **Status tracking** (ACTIVE, INACTIVE, etc.)
- **Bullhorn integration** fields

## Usage

### In TypeScript/Node.js Projects

```typescript
import { PrismaClient } from '@referral-system/database'

const prisma = new PrismaClient()

// Query candidates
const candidates = await prisma.candidate.findMany({
  where: { status: 'ACTIVE' },
  include: { matches: true }
})

// Create a match
const match = await prisma.match.create({
  data: {
    candidateId: 'abc123',
    positionId: 'xyz789',
    overallScore: 85,
    status: 'PENDING'
  }
})
```

### In Python Projects

Use raw SQL or psycopg2:

```python
import psycopg2

conn = psycopg2.connect(
    host='localhost',
    database='referral_system',
    user='referral_user',
    password='referral_pass'
)

cursor = conn.cursor()
cursor.execute('SELECT * FROM "Candidate" WHERE status = %s', ('ACTIVE',))
```

## Development

### Prerequisites

- Node.js 20+
- PostgreSQL 15+ with pgvector extension

### Setup

```bash
# Generate Prisma Client
npm run db:generate

# Run migrations
npm run db:migrate

# Open Prisma Studio (database GUI)
npm run db:studio

# Push schema changes (dev only)
npm run db:push

# Seed database
npm run db:seed
```

### Creating Migrations

```bash
# Create a new migration
npx prisma migrate dev --name add_new_field

# Apply migrations in production
npx prisma migrate deploy
```

### Prisma Studio

Visual database editor:

```bash
npm run db:studio
# Opens at http://localhost:5555
```

## Schema Structure

```prisma
model Candidate {
  id                String   @id @default(cuid())
  bullhornId        String   @unique
  firstName         String
  lastName          String
  email             String?
  skills            String[]
  profileEmbedding  Unsupported("vector(384)")?
  matches           Match[]
  status            String
  createdAt         DateTime @default(now())
  updatedAt         DateTime @updatedAt
}

model Position {
  id                   String   @id @default(cuid())
  title                String
  requiredSkills       String[]
  preferredSkills      String[]
  descriptionEmbedding Unsupported("vector(384)")?
  matches              Match[]
  status               String
  createdAt            DateTime @default(now())
  updatedAt            DateTime @updatedAt
}

model Match {
  id              String   @id @default(cuid())
  candidateId     String
  positionId      String
  overallScore    Int
  semanticScore   Int?
  skillMatchScore Int?
  candidate       Candidate @relation(fields: [candidateId], references: [id])
  position        Position  @relation(fields: [positionId], references: [id])
  status          String
  createdAt       DateTime @default(now())
  updatedAt       DateTime @updatedAt

  @@unique([candidateId, positionId])
}
```

## pgvector Extension

### Installation

```sql
CREATE EXTENSION IF NOT EXISTS vector;
```

### Vector Operations

```sql
-- Cosine similarity
SELECT 1 - (embedding1 <=> embedding2) as similarity
FROM ...

-- Euclidean distance
SELECT embedding1 <-> embedding2 as distance
FROM ...

-- Order by similarity
SELECT *
FROM "Candidate"
ORDER BY "profileEmbedding" <=> '[0.1, 0.2, ...]'::vector
LIMIT 10;
```

## Environment Variables

```env
DATABASE_URL=postgresql://referral_user:referral_pass@localhost:5432/referral_system
```

## Testing

```bash
# Generate Prisma Client for testing
npm run db:generate

# Run tests
npm test
```

## Deployment

### Migrations in Production

```bash
# Deploy pending migrations
npx prisma migrate deploy

# Check migration status
npx prisma migrate status
```

### Connection Pooling

For production, use connection pooling:

```typescript
import { PrismaClient } from '@referral-system/database'

const prisma = new PrismaClient({
  datasources: {
    db: {
      url: process.env.DATABASE_URL,
    },
  },
  log: process.env.NODE_ENV === 'development' ? ['query', 'error'] : ['error'],
})
```

## Common Operations

### Cleanup Data

```bash
# Clean all data (dev only)
npm run db:cleanup

# Clean test data only
npm run db:cleanup:test
```

### Reset Database

```bash
# Drop database, create, and migrate (dev only)
npx prisma migrate reset
```

### Generate Client After Schema Changes

```bash
npm run db:generate
```

## Related Components

- **API**: `apps/api` - Main consumer of database
- **ML Service**: `services/ml-service` - Direct SQL queries for performance
- **ETL Service**: `services/etl-service` - Bulk data loading
