# API Service

Backend REST API for the Referral System.

## Purpose

Provides RESTful endpoints for managing candidates, positions, and matches. Handles authentication, data validation, and business logic.

## Tech Stack

- **Runtime**: Node.js 20+
- **Framework**: Express.js
- **Language**: TypeScript
- **Database**: PostgreSQL (via Prisma)
- **ORM**: Prisma Client

## Architecture

```
src/
├── controllers/     # Request handlers
├── services/        # Business logic
├── routes/          # API route definitions
├── middleware/      # Express middleware
├── types/           # TypeScript type definitions
├── utils/           # Utility functions
└── config/          # Configuration
```

## Development

### Prerequisites

- Node.js 20+
- PostgreSQL database running
- Database schema migrated

### Setup

```bash
# Install dependencies (from monorepo root)
npm install

# Generate Prisma client
npm run db:generate

# Run migrations
npm run db:migrate
```

### Running Locally

```bash
# Development mode with hot reload
npm run dev --filter=@referral-system/api

# Build
npm run build --filter=@referral-system/api

# Production mode
npm run start --filter=@referral-system/api
```

### Environment Variables

```env
PORT=3001
DATABASE_URL=postgresql://user:pass@localhost:5432/referral_system
NODE_ENV=development
```

## API Endpoints

### Candidates
- `GET /api/candidates` - List all candidates
- `GET /api/candidates/:id` - Get candidate by ID
- `POST /api/candidates` - Create new candidate
- `PUT /api/candidates/:id` - Update candidate
- `DELETE /api/candidates/:id` - Delete candidate

### Positions
- `GET /api/positions` - List all positions
- `GET /api/positions/:id` - Get position by ID
- `POST /api/positions` - Create new position
- `PUT /api/positions/:id` - Update position
- `DELETE /api/positions/:id` - Delete position

### Matches
- `GET /api/matches` - List all matches
- `GET /api/matches/:id` - Get match by ID
- `POST /api/matches/generate` - Trigger matching algorithm

## Testing

```bash
# Run tests
npm run test --filter=@referral-system/api

# Run tests in watch mode
npm run test:watch --filter=@referral-system/api
```

## Deployment

The API is containerized with Docker and deployed alongside other services using docker-compose.

```bash
# Build Docker image
docker build -t referral-system-api .

# Run container
docker run -p 3001:3001 referral-system-api
```

## Related Components

- **Database Package**: `@referral-system/database` - Prisma schema and client
- **Shared Package**: `@referral-system/shared` - Shared types and utilities
- **ML Service**: `services/ml-service` - Matching algorithm implementation
