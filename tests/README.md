# Tests

End-to-end and integration tests for the Referral System.

## Purpose

This directory contains system-level tests that verify:
- **End-to-end workflows** across multiple services
- **Integration between components**
- **API contract compliance**
- **Performance benchmarks**

## Structure

```
tests/
├── e2e/              # End-to-end tests
│   ├── matching/     # Full matching workflow tests
│   ├── sync/         # Data sync tests
│   └── api/          # API integration tests
├── integration/      # Integration tests
│   ├── database/     # Database integration
│   ├── services/     # Service-to-service integration
│   └── external/     # External API mocks
├── fixtures/         # Test data and fixtures
│   ├── candidates/   # Sample candidate data
│   ├── positions/    # Sample position data
│   └── matches/      # Expected match results
└── performance/      # Performance and load tests
    ├── matching/     # Matching performance tests
    └── sync/         # Sync performance tests
```

## Test Types

### Unit Tests

Located within each component:
- `apps/api/tests/` - API unit tests
- `services/ml-service/tests/` - ML service unit tests
- `packages/*/tests/` - Package unit tests

### Integration Tests

Located in `tests/integration/`:
- Database operations
- Service communication
- External API mocking

### End-to-End Tests

Located in `tests/e2e/`:
- Complete user workflows
- Multi-service scenarios
- Real database operations

### Performance Tests

Located in `tests/performance/`:
- Load testing
- Stress testing
- Benchmark comparisons

## Running Tests

### Prerequisites

```bash
# Ensure all services are running
npm run infra:up

# Seed test data
npm run db:seed
```

### Run All Tests

```bash
# Run all tests in monorepo
npm test

# Run only integration tests
npm test -- tests/integration

# Run only e2e tests
npm test -- tests/e2e
```

### Run Specific Test Suites

```bash
# TypeScript tests (Jest)
npx jest tests/e2e/matching

# Python tests (pytest)
pytest tests/integration/ml-service
```

## Writing Tests

### E2E Test Example

```typescript
// tests/e2e/matching/full-workflow.test.ts
import { PrismaClient } from '@referral-system/database'
import axios from 'axios'

describe('Full Matching Workflow', () => {
  let prisma: PrismaClient

  beforeAll(async () => {
    prisma = new PrismaClient()
    // Setup test data
  })

  afterAll(async () => {
    // Cleanup
    await prisma.$disconnect()
  })

  test('should sync, generate embeddings, and create matches', async () => {
    // 1. Sync candidates
    const syncResponse = await axios.post('http://localhost:3001/api/sync/bullhorn')
    expect(syncResponse.status).toBe(200)

    // 2. Generate embeddings
    const embeddingResponse = await axios.post('http://localhost:8000/embeddings/generate')
    expect(embeddingResponse.status).toBe(200)

    // 3. Trigger matching
    const matchResponse = await axios.post('http://localhost:8000/match/find', {
      minScore: 70
    })
    expect(matchResponse.status).toBe(200)

    // 4. Verify matches in database
    const matches = await prisma.match.findMany({
      where: { overallScore: { gte: 70 } }
    })
    expect(matches.length).toBeGreaterThan(0)
  })
})
```

### Integration Test Example

```typescript
// tests/integration/services/ml-api.test.ts
import axios from 'axios'

describe('ML Service Integration', () => {
  test('should generate embeddings', async () => {
    const response = await axios.post('http://localhost:8000/embeddings/batch', {
      texts: ['Software Engineer with Python experience']
    })

    expect(response.status).toBe(200)
    expect(response.data.embeddings).toHaveLength(1)
    expect(response.data.embeddings[0]).toHaveLength(384)
  })

  test('should find matches', async () => {
    const response = await axios.post('http://localhost:8000/match/find', {
      minScore: 70
    })

    expect(response.status).toBe(200)
    expect(response.data).toHaveProperty('positionsProcessed')
    expect(response.data).toHaveProperty('totalMatches')
  })
})
```

## Test Data

### Fixtures

Sample data for testing located in `tests/fixtures/`:

```typescript
// tests/fixtures/candidates/sample-candidates.json
[
  {
    "firstName": "John",
    "lastName": "Doe",
    "email": "john@example.com",
    "skills": ["JavaScript", "React", "Node.js"],
    "experienceLevel": "SENIOR"
  }
]
```

### Loading Fixtures

```typescript
import sampleCandidates from '../fixtures/candidates/sample-candidates.json'

await prisma.candidate.createMany({
  data: sampleCandidates
})
```

## Test Environment

### Environment Variables

```env
# Test database
TEST_DATABASE_URL=postgresql://referral_user:referral_pass@localhost:5432/referral_system_test

# Test services
ML_SERVICE_URL=http://localhost:8000
API_URL=http://localhost:3001

# Test mode flags
NODE_ENV=test
SKIP_EXTERNAL_APIS=true
```

### Docker Compose for Testing

```bash
# Start services in test mode
docker-compose -f docker-compose.test.yml up -d

# Run tests
npm test

# Teardown
docker-compose -f docker-compose.test.yml down -v
```

## Continuous Integration

### GitHub Actions

```yaml
# .github/workflows/test.yml
name: Tests

on: [push, pull_request]

jobs:
  test:
    runs-on: ubuntu-latest

    services:
      postgres:
        image: pgvector/pgvector:pg15
        env:
          POSTGRES_PASSWORD: test
        ports:
          - 5432:5432

    steps:
      - uses: actions/checkout@v3
      - uses: actions/setup-node@v3
      - run: npm install
      - run: npm run db:migrate
      - run: npm test
```

## Performance Testing

### Load Test Example

```typescript
// tests/performance/matching/load.test.ts
import { performance } from 'perf_hooks'

describe('Matching Performance', () => {
  test('should handle 1000 candidates in under 60 seconds', async () => {
    const start = performance.now()

    const response = await axios.post('http://localhost:8000/match/find', {
      minScore: 70
    })

    const duration = (performance.now() - start) / 1000

    expect(response.status).toBe(200)
    expect(duration).toBeLessThan(60)
  })
})
```

## Test Coverage

### Generating Coverage Reports

```bash
# TypeScript coverage
npx jest --coverage

# Python coverage
pytest --cov=src --cov-report=html tests/
```

### Coverage Goals

- **Unit tests**: >80% coverage
- **Integration tests**: Key workflows covered
- **E2E tests**: Critical user paths covered

## Mocking

### External Services

Mock external APIs in tests:

```typescript
// tests/mocks/bullhorn-api.ts
import nock from 'nock'

export function mockBullhornAPI() {
  nock('https://rest.bullhornstaffing.com')
    .post('/oauth/token')
    .reply(200, { access_token: 'test-token' })

  nock('https://rest.bullhornstaffing.com')
    .get('/search/Candidate')
    .reply(200, { data: [] })
}
```

## Troubleshooting

### Tests Timing Out

- Increase timeout: `jest.setTimeout(30000)`
- Check service health before running tests
- Verify database connection

### Flaky Tests

- Use proper cleanup in `afterEach`/`afterAll`
- Avoid hardcoded delays, use `waitFor`
- Ensure test isolation (no shared state)

### Database State Issues

```bash
# Reset test database between runs
npm run db:reset -- --test

# Or use transactions
await prisma.$transaction(async (tx) => {
  // Test operations
})
```

## Future Enhancements

- [ ] Visual regression testing (screenshots)
- [ ] API contract testing (Pact)
- [ ] Chaos engineering tests
- [ ] Security testing (penetration tests)
- [ ] Accessibility testing (a11y)

## Related Components

- **All Services** - Tests verify integration between all components
- **CI/CD** - `.github/workflows` - Automated test execution
