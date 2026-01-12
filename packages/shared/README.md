# Shared Package

Shared utilities, types, and constants used across the Referral System monorepo.

## Purpose

Provides reusable code to avoid duplication across applications:
- **TypeScript types** and interfaces
- **Constants** and enumerations
- **Utility functions**
- **Validation helpers**
- **Common configurations**

## Usage

```typescript
import { STATUS, type Candidate, type Position } from '@referral-system/shared'

// Use constants
if (candidate.status === STATUS.CANDIDATE.ACTIVE) {
  // ...
}

// Use types
const candidate: Candidate = {
  id: '123',
  firstName: 'John',
  lastName: 'Doe',
  // ...
}
```

## Development

### Building

```bash
# Build the package
npm run build --filter=@referral-system/shared

# Watch mode for development
npm run dev --filter=@referral-system/shared
```

### Adding New Exports

1. Add your code to `src/`
2. Export from `src/index.ts`
3. Rebuild: `npm run build`
4. Dependent packages will pick up changes

## What's Included

### Types
- `Candidate` - Candidate/consultant type definitions
- `Position` - Job position type definitions
- `Match` - Match result type definitions
- API request/response types

### Constants
- `STATUS` - Status enumerations for all entities
- `EXPERIENCE_LEVELS` - Experience level mappings
- Configuration defaults

### Utilities
- Date formatting helpers
- String manipulation functions
- Validation utilities

## Related Components

- **API**: `apps/api` - Primary consumer
- **Web Console**: `apps/web-consultant` - UI consumer
- **Database**: `packages/database` - Type compatibility
