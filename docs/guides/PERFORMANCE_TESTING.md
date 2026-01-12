# Performance Testing Guide

This guide explains how to test the batch upsert performance improvements.

## Overview

We've created performance testing scripts to validate the 10-20x performance improvement from the sequential processing fix.

## Available Tests

| Test Number | Dataset Size | Records | Use Case |
|-------------|--------------|---------|----------|
| 0 | Small | 100 | Quick validation + comparison |
| 1 | Medium | 1,000 | Typical incremental sync |
| 2 | Large | 10,000 | Full sync scenario |
| 3 | Very Large | 100,000 | Stress test |

## Running Tests

### Prerequisites

Ensure your database is running:
```bash
docker-compose up -d postgres redis
```

### Run a Performance Test

```bash
# Test with 100 records (includes sequential comparison)
npm run test:performance 0

# Test with 1,000 records
npm run test:performance 1

# Test with 10,000 records
npm run test:performance 2

# Test with 100,000 records (stress test)
npm run test:performance 3
```

### Cleanup Test Data

After testing, clean up the generated test data:
```bash
npm run test:cleanup
```

## What the Tests Do

### 1. Consultant Test
- Simple structure (no arrays/JSON)
- Tests basic batch upsert performance
- For small datasets (100), compares batch vs sequential

### 2. Candidate Test
- Complex structure with array fields (skills)
- Tests PostgreSQL array handling
- Validates text[] type casting

### 3. Position Test
- Complex structure with multiple array fields
- Tests requiredSkills and preferredSkills arrays
- Validates date field handling

## Expected Results

### Small Dataset (100 records)

**Batch Approach:**
- Duration: ~100-200ms
- Speed: 500-1000 records/second

**Sequential Approach (old):**
- Duration: ~6-10 seconds
- Speed: 10-20 records/second

**Speedup: 30-50x faster**

### Medium Dataset (1,000 records)

**Batch Approach:**
- Duration: ~1-2 seconds
- Speed: 500-1000 records/second

**Sequential (estimated):**
- Duration: ~60-100 seconds

**Speedup: 30-50x faster**

### Large Dataset (10,000 records)

**Batch Approach:**
- Duration: ~10-20 seconds
- Speed: 500-1000 records/second

**Sequential (estimated):**
- Duration: ~600-1000 seconds (10-16 minutes)

**Speedup: 30-50x faster**

### Very Large Dataset (100,000 records)

**Batch Approach:**
- Duration: ~100-200 seconds (1.5-3 minutes)
- Speed: 500-1000 records/second

**Sequential (estimated):**
- Duration: ~6000-10000 seconds (1.5-2.7 hours)

**Speedup: 30-50x faster**

## Sample Output

```
 PERFORMANCE TEST: Batch Upsert vs Sequential
============================================================

Running: Small Dataset (100 records)


TEST 1: CONSULTANTS
============================================================
Testing Consultant - 100 records
============================================================
Complete in 156ms
   - Created: 100
   - Updated: 0
   - Speed: 641 records/second
   - Avg: 1.56ms per record

============================================================
Testing SEQUENTIAL Consultant - 100 records
============================================================
 WARNING: This will be SLOW - testing old approach
   Progress: 100/100
Complete in 6842ms
   - Created: 0
   - Updated: 100
   - Speed: 15 records/second
   - Avg: 68.42ms per record

COMPARISON:
   Batch approach: 156ms
   Sequential approach: 6842ms
   Speedup: 43.9x faster
```

## Performance Metrics Explained

### Records/Second
How many records are processed per second. Higher is better.

### Avg ms per record
Average time to process one record. Lower is better.

### Created vs Updated
- **Created**: New records inserted
- **Updated**: Existing records updated (via ON CONFLICT)

On first run, all records are created. On subsequent runs, all are updated.

## Database Impact

### Connection Pool Usage
- Batch approach: Uses 1-2 connections (sequential chunks)
- Sequential: Uses 1 connection (but blocks for long time)

### Database Load
- Batch approach: Short bursts of high activity
- Sequential: Sustained low-level activity

### Transaction Size
- Batch approach: 100 records per transaction (chunked)
- Sequential: 1 record per transaction

## Troubleshooting

### Error: Connection Pool Exhausted
If you see this error:
1. Check `src/config/database.ts` - pool size should be 15
2. Reduce chunk size in `batchUpsert()` call
3. Ensure no other processes are holding connections

### Error: Query Too Large
If you see "query string too long":
1. Reduce chunk size from 100 to 50
2. Check for extremely long text fields

### Slow Performance
If batch approach is slower than expected:
1. Check database CPU/memory usage
2. Verify PostgreSQL is running locally (not remote)
3. Check for missing indexes on `bullhornId`
4. Ensure no other heavy queries running

## Comparison with Production

Real Bullhorn sync will include:
- API call time (not tested here)
- Network latency (not tested here)
- Data transformation overhead (minimal)

The database operations shown in these tests represent the **true bottleneck** that was fixed.

## Next Steps

After validating performance:
1. Test with real Bullhorn sync
2. Monitor production logs for timing
3. Consider adding database indexes if queries are slow
4. Adjust chunk size based on actual data characteristics

## Cleanup

Always cleanup test data after testing:
```bash
npm run test:cleanup
```

This removes all records with `bullhornId` starting with `test-`.

## Safety

Test data uses:
- Prefix `test-` in all `bullhornId` fields
- Test email domains (`@test.com`)
- Clearly marked test data

This ensures test data doesn't interfere with production data.
