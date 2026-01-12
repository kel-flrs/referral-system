# Bullhorn Client

TypeScript client library for Bullhorn REST API integration.

## Purpose

Provides a type-safe, easy-to-use client for interacting with Bullhorn ATS:
- **OAuth 2.0 authentication** with automatic token refresh
- **Type-safe API methods** for all Bullhorn entities
- **Error handling** and retry logic
- **Rate limiting** support

## Usage

```typescript
import { BullhornClient } from '@referral-system/bullhorn-client'

const client = new BullhornClient({
  clientId: process.env.BULLHORN_CLIENT_ID,
  clientSecret: process.env.BULLHORN_CLIENT_SECRET,
  username: process.env.BULLHORN_USERNAME,
  password: process.env.BULLHORN_PASSWORD,
  restUrl: process.env.BULLHORN_REST_URL
})

// Authenticate
await client.authenticate()

// Fetch candidates
const candidates = await client.searchCandidates({
  where: 'status="Active"',
  fields: ['id', 'firstName', 'lastName', 'email'],
  count: 100
})

// Fetch job orders
const jobs = await client.searchJobOrders({
  where: 'status="Open"',
  fields: ['id', 'title', 'requiredSkills'],
  count: 100
})

// Get single entity
const candidate = await client.getCandidate('12345', [
  'id',
  'firstName',
  'lastName',
  'skills'
])
```

## Features

### OAuth 2.0 Flow

Handles the full OAuth flow:
1. Login to get authorization code
2. Exchange code for access token
3. Automatic token refresh when expired
4. Session management

### Pagination Support

```typescript
// Fetch all candidates with pagination
let start = 0
const count = 500

while (true) {
  const response = await client.searchCandidates({
    where: 'status="Active"',
    start,
    count
  })

  if (response.data.length === 0) break

  // Process batch
  processBatch(response.data)

  start += count
}
```

### Error Handling

```typescript
try {
  const candidate = await client.getCandidate('12345')
} catch (error) {
  if (error.code === 'BULLHORN_AUTH_ERROR') {
    // Handle authentication error
  } else if (error.code === 'BULLHORN_NOT_FOUND') {
    // Handle not found
  }
}
```

## Development

### Building

```bash
npm run build --filter=@referral-system/bullhorn-client
```

### Configuration

```env
BULLHORN_CLIENT_ID=your_client_id
BULLHORN_CLIENT_SECRET=your_client_secret
BULLHORN_USERNAME=your_username
BULLHORN_PASSWORD=your_password
BULLHORN_REST_URL=https://rest.bullhornstaffing.com/rest-services/
```

## API Methods

### Candidates (Consultants)
- `searchCandidates(query)` - Search/list candidates
- `getCandidate(id, fields)` - Get single candidate
- `createCandidate(data)` - Create new candidate
- `updateCandidate(id, data)` - Update candidate

### Job Orders
- `searchJobOrders(query)` - Search/list job orders
- `getJobOrder(id, fields)` - Get single job order
- `createJobOrder(data)` - Create new job order
- `updateJobOrder(id, data)` - Update job order

### Placements
- `searchPlacements(query)` - Search/list placements
- `getPlacement(id, fields)` - Get single placement

## Rate Limiting

Bullhorn API has rate limits. The client handles this automatically:
- Tracks requests per minute
- Implements backoff strategy
- Retries failed requests

## Testing

Use the Bullhorn Mock service for local testing:

```typescript
const client = new BullhornClient({
  restUrl: 'http://localhost:8082', // Bullhorn mock
  // ...
})
```

## Related Components

- **Bullhorn Mock**: `apps/bullhorn-mock` - Local Bullhorn API simulator
- **ETL Service**: `services/etl-service` - Uses this client for syncing
- **API**: `apps/api` - May use for real-time queries
