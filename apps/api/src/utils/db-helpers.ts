import { logger } from '../config/logger';
import { randomUUID } from 'crypto';
import { Prisma } from '@prisma/client';

// ============================================================================
// TYPES & INTERFACES
// ============================================================================

interface UpsertResult {
  created: number;
  updated: number;
}

interface UpsertWithRetryResult extends UpsertResult {
  failed: number;
}

type DatabaseRecord = Record<string, any>;

interface BatchUpsertOptions<T extends DatabaseRecord> {
  /** Prisma client instance */
  prisma: any;
  /** Database table name (case-sensitive, e.g., 'Consultant') */
  tableName: string;
  /** Array of records to upsert */
  data: T[];
  /** Unique column name for conflict detection (e.g., 'bullhornId') */
  conflictColumn: string;
  /** List of columns to update when conflict occurs */
  updateColumns: string[];
  /** Number of records per batch (default: 100) */
  chunkSize?: number;
}

interface BatchUpsertWithRetryOptions<T extends DatabaseRecord> extends BatchUpsertOptions<T> {
  /** Maximum retry attempts per chunk (default: 3) */
  maxRetries?: number;
}

// ============================================================================
// TYPE-SAFE PRISMA MODEL HELPERS
// ============================================================================

/**
 * Type-safe batch upsert options using Prisma model types
 * This ensures column names are validated against your schema
 */
type PrismaModelName = Prisma.ModelName;

type ModelUpsertOptions<TModel extends PrismaModelName> = {
  /** Prisma client instance */
  prisma: any;
  /** Prisma model name (type-safe) */
  model: TModel;
  /** Array of records to upsert */
  data: any[];
  /** Unique column name for conflict detection */
  conflictColumn: keyof Prisma.TypeMap['model'][TModel]['fields'];
  /** List of columns to update when conflict occurs */
  updateColumns: Array<keyof Prisma.TypeMap['model'][TModel]['fields']>;
  /** Number of records per batch (default: 100) */
  chunkSize?: number;
};

type ModelUpsertWithRetryOptions<TModel extends PrismaModelName> = ModelUpsertOptions<TModel> & {
  /** Maximum retry attempts per chunk (default: 3) */
  maxRetries?: number;
};

// ============================================================================
// UTILITY FUNCTIONS
// ============================================================================

/**
 * Generate a unique identifier compatible with Prisma's cuid format
 *
 * @returns A UUID string without dashes
 */
function generateCuid(): string {
  return randomUUID().replace(/-/g, '');
}

/**
 * Split an array into smaller chunks for batch processing
 *
 * @example
 * chunkArray([1,2,3,4,5], 2) // [[1,2], [3,4], [5]]
 */
export function chunkArray<T>(array: T[], chunkSize: number): T[][] {
  const chunks: T[][] = [];

  for (let i = 0; i < array.length; i += chunkSize) {
    chunks.push(array.slice(i, i + chunkSize));
  }

  return chunks;
}

/**
 * Ensure all records have required timestamp and ID fields
 *
 * @param records - Array of database records
 * @returns Records with id, createdAt, and updatedAt fields
 */
function ensureRequiredFields<T extends DatabaseRecord>(records: T[]): T[] {
  const now = new Date();

  return records.map(record => ({
    id: record.id || generateCuid(),
    createdAt: record.createdAt || now,
    updatedAt: record.updatedAt || now,
    ...record,
  }));
}

// ============================================================================
// POSTGRESQL VALUE FORMATTING
// ============================================================================

/**
 * Convert a JavaScript value to PostgreSQL-compatible SQL string
 *
 * Handles:
 * - NULL values
 * - Strings (with proper escaping)
 * - Booleans (TRUE/FALSE)
 * - Numbers
 * - Dates (ISO format)
 * - Arrays (PostgreSQL array syntax)
 * - Objects (JSONB format)
 */
function formatValueForPostgres(value: any): string {
  // Handle null/undefined
  if (value === null || value === undefined) {
    return 'NULL';
  }

  // Handle strings - escape single quotes
  if (typeof value === 'string') {
    const escapedValue = value.replace(/'/g, "''");
    return `'${escapedValue}'`;
  }

  // Handle booleans
  if (typeof value === 'boolean') {
    return value ? 'TRUE' : 'FALSE';
  }

  // Handle numbers
  if (typeof value === 'number') {
    return value.toString();
  }

  // Handle dates - convert to ISO string
  if (value instanceof Date) {
    return `'${value.toISOString()}'`;
  }

  // Handle arrays - PostgreSQL array format: '{value1,value2}'
  if (Array.isArray(value)) {
    const formattedItems = value.map(item => {
      if (typeof item === 'string') {
        // Escape quotes in array string elements
        return `"${item.replace(/"/g, '\\"')}"`;
      }
      return item;
    });
    return `'{${formattedItems.join(',')}}'`;
  }

  // Handle objects - convert to JSON
  if (typeof value === 'object') {
    const jsonString = JSON.stringify(value);
    const escapedJson = jsonString.replace(/'/g, "''");
    return `'${escapedJson}'`;
  }

  // Fallback: convert to string
  return String(value);
}

/**
 * Get PostgreSQL type cast operator for a value
 *
 * Returns type cast string (e.g., '::text[]', '::jsonb') or empty string
 */
function getPostgresTypeCast(value: any): string {
  if (Array.isArray(value)) {
    return '::text[]';
  }

  if (typeof value === 'object' && value !== null && !(value instanceof Date)) {
    return '::jsonb';
  }

  return '';
}

// ============================================================================
// SQL QUERY BUILDERS
// ============================================================================

/**
 * Build the VALUES clause for INSERT statement
 *
 * @example
 * buildValuesClause([{name: 'John', age: 30}], ['name', 'age'])
 * // Returns: "('John', 30)"
 */
function buildValuesClause(records: DatabaseRecord[], columns: string[]): string {
  const rowValues = records.map(record => {
    const values = columns.map(col => formatValueForPostgres(record[col]));
    return `(${values.join(', ')})`;
  });

  return rowValues.join(',\n      ');
}

/**
 * Build the UPDATE SET clause for ON CONFLICT
 *
 * @example
 * buildUpdateClause(['name', 'age'], sampleRecord)
 * // Returns: '"name" = EXCLUDED."name", "age" = EXCLUDED."age"'
 */
function buildUpdateClause(
  columnsToUpdate: string[],
  sampleRecord: DatabaseRecord
): string {
  const setStatements = columnsToUpdate.map(columnName => {
    const sampleValue = sampleRecord[columnName];
    const typeCast = getPostgresTypeCast(sampleValue);

    return `"${columnName}" = EXCLUDED."${columnName}"${typeCast}`;
  });

  return setStatements.join(',\n        ');
}

/**
 * Build complete PostgreSQL INSERT ... ON CONFLICT query
 */
function buildUpsertQuery(
  tableName: string,
  columns: string[],
  valuesClause: string,
  conflictColumn: string,
  updateClause: string
): string {
  const columnNames = columns.map(col => `"${col}"`).join(', ');

  return `
    INSERT INTO "${tableName}" (${columnNames})
    VALUES
      ${valuesClause}
    ON CONFLICT ("${conflictColumn}")
    DO UPDATE SET
      ${updateClause},
      "updatedAt" = CURRENT_TIMESTAMP
    RETURNING (xmax = 0) AS inserted;
  `;
}

// ============================================================================
// RESULT PROCESSING
// ============================================================================

/**
 * Count created vs updated records from PostgreSQL RETURNING clause
 *
 * PostgreSQL's xmax = 0 indicates a newly inserted row
 * xmax > 0 indicates an updated row
 */
function countCreatedAndUpdated(results: Array<{ inserted: boolean }>): UpsertResult {
  const created = results.filter(row => row.inserted).length;
  const updated = results.filter(row => !row.inserted).length;

  return { created, updated };
}

// ============================================================================
// MAIN BATCH UPSERT FUNCTION
// ============================================================================

/**
 * Batch upsert records using PostgreSQL's INSERT ... ON CONFLICT
 *
 * This function provides high-performance bulk upserts by:
 * 1. Chunking large datasets to avoid query size limits
 * 2. Using raw SQL for optimal performance
 * 3. Automatically handling IDs and timestamps
 * 4. Returning accurate created/updated counts
 *
 * @param options - Configuration object with named parameters
 * @returns Object containing counts of created and updated records
 *
 * @example
 * // Using options object (recommended - clearer intent)
 * const result = await performBatchUpsert({
 *   prisma,
 *   tableName: 'Consultant',
 *   data: consultants,
 *   conflictColumn: 'bullhornId',
 *   updateColumns: ['firstName', 'lastName', 'email'],
 *   chunkSize: 100
 * });
 *
 * @example
 * // Minimal example with defaults
 * const result = await performBatchUpsert({
 *   prisma,
 *   tableName: 'ConsultantActivity',
 *   data: activities,
 *   conflictColumn: 'bullhornId',
 *   updateColumns: ['activityType', 'subject', 'activityDate']
 * });
 * console.log(`Created: ${result.created}, Updated: ${result.updated}`);
 */
export async function performBatchUpsert<T extends DatabaseRecord>(
  options: BatchUpsertOptions<T>
): Promise<UpsertResult>;

/**
 * @deprecated Use options object instead for better readability
 * @internal Legacy signature - maintained for backward compatibility
 */
export async function performBatchUpsert<T extends DatabaseRecord>(
  prisma: any,
  tableName: string,
  data: T[],
  conflictColumn: string,
  updateColumns: string[],
  chunkSize?: number
): Promise<UpsertResult>;

// Implementation
export async function performBatchUpsert<T extends DatabaseRecord>(
  optionsOrPrisma: BatchUpsertOptions<T> | any,
  tableName?: string,
  data?: T[],
  conflictColumn?: string,
  updateColumns?: string[],
  chunkSize: number = 100
): Promise<UpsertResult> {
  // Handle both signatures: options object or individual parameters
  let prisma: any;
  let table: string;
  let records: T[];
  let conflict: string;
  let updates: string[];
  let chunk: number;

  if (tableName && data && conflictColumn && updateColumns) {
    // Legacy signature: individual parameters
    prisma = optionsOrPrisma;
    table = tableName;
    records = data;
    conflict = conflictColumn;
    updates = updateColumns;
    chunk = chunkSize;
  } else {
    // New signature: options object
    const opts = optionsOrPrisma as BatchUpsertOptions<T>;
    prisma = opts.prisma;
    table = opts.tableName;
    records = opts.data;
    conflict = opts.conflictColumn;
    updates = opts.updateColumns;
    chunk = opts.chunkSize ?? 100;
  }
  // Early return for empty data
  if (records.length === 0) {
    return { created: 0, updated: 0 };
  }

  let totalCreated = 0;
  let totalUpdated = 0;

  // Step 1: Ensure all records have required fields (id, timestamps)
  const recordsWithDefaults = ensureRequiredFields(records);

  // Step 2: Split into chunks to avoid query size limits
  const chunks = chunkArray(recordsWithDefaults, chunk);

  // Step 3: Get column names from first record
  const columns = Object.keys(recordsWithDefaults[0]);

  // Step 4: Process each chunk
  for (const [chunkIndex, currentChunk] of chunks.entries()) {
    try {
      // Build SQL query components
      const valuesClause = buildValuesClause(currentChunk, columns);
      const updateClause = buildUpdateClause(updates, currentChunk[0]);
      const query = buildUpsertQuery(
        table,
        columns,
        valuesClause,
        conflict,
        updateClause
      );

      // Execute query
      const results = await prisma.$queryRawUnsafe(query) as Array<{ inserted: boolean }>;

      // Count results
      const { created, updated } = countCreatedAndUpdated(results);
      totalCreated += created;
      totalUpdated += updated;

      // Log progress
      logger.debug(
        `Batch ${chunkIndex + 1}/${chunks.length}: ${created} created, ${updated} updated`
      );

    } catch (error) {
      logger.error(
        `Failed to process chunk ${chunkIndex + 1}/${chunks.length}:`,
        error
      );
      throw error;
    }
  }

  return {
    created: totalCreated,
    updated: totalUpdated
  };
}

// ============================================================================
// BATCH UPSERT WITH RETRY
// ============================================================================

/**
 * Batch upsert with automatic retry logic for transient failures
 *
 * Useful for handling temporary network issues, deadlocks, or connection timeouts
 *
 * @param options - Configuration object with named parameters
 * @returns Object with created, updated, and failed counts
 *
 * @example
 * // Using options object (recommended)
 * const result = await batchUpsertWithRetry({
 *   prisma,
 *   tableName: 'Consultant',
 *   data: consultants,
 *   conflictColumn: 'bullhornId',
 *   updateColumns: ['firstName', 'lastName'],
 *   chunkSize: 100,
 *   maxRetries: 3
 * });
 *
 * if (result.failed > 0) {
 *   console.error(`Failed to process ${result.failed} records`);
 * }
 */
export async function batchUpsertWithRetry<T extends DatabaseRecord>(
  options: BatchUpsertWithRetryOptions<T>
): Promise<UpsertWithRetryResult>;

/**
 * @deprecated Use options object instead for better readability
 * @internal Legacy signature - maintained for backward compatibility
 */
export async function batchUpsertWithRetry<T extends DatabaseRecord>(
  prisma: any,
  tableName: string,
  data: T[],
  conflictColumn: string,
  updateColumns: string[],
  chunkSize?: number,
  maxRetries?: number
): Promise<UpsertWithRetryResult>;

// Implementation
export async function batchUpsertWithRetry<T extends DatabaseRecord>(
  optionsOrPrisma: BatchUpsertWithRetryOptions<T> | any,
  tableName?: string,
  data?: T[],
  conflictColumn?: string,
  updateColumns?: string[],
  chunkSize: number = 100,
  maxRetries: number = 3
): Promise<UpsertWithRetryResult> {
  // Handle both signatures
  let options: BatchUpsertWithRetryOptions<T>;

  if (tableName && data && conflictColumn && updateColumns) {
    // Legacy signature
    options = {
      prisma: optionsOrPrisma,
      tableName,
      data,
      conflictColumn,
      updateColumns,
      chunkSize,
      maxRetries
    };
  } else {
    // New signature
    options = optionsOrPrisma as BatchUpsertWithRetryOptions<T>;
  }
  // Early return for empty data
  if (options.data.length === 0) {
    return { created: 0, updated: 0, failed: 0 };
  }

  let totalCreated = 0;
  let totalUpdated = 0;
  let totalFailed = 0;

  const retryChunkSize = options.chunkSize ?? 100;
  const maxRetryAttempts = options.maxRetries ?? 3;
  const chunks = chunkArray(options.data, retryChunkSize);

  // Process each chunk with retry logic
  for (const [chunkIndex, chunk] of chunks.entries()) {
    let retries = 0;
    let success = false;

    while (!success && retries < maxRetryAttempts) {
      try {
        // Attempt to upsert this chunk
        const result = await performBatchUpsert({
          prisma: options.prisma,
          tableName: options.tableName,
          data: chunk,
          conflictColumn: options.conflictColumn,
          updateColumns: options.updateColumns,
          chunkSize: retryChunkSize
        });

        totalCreated += result.created;
        totalUpdated += result.updated;
        success = true;

        logger.debug(
          `Chunk ${chunkIndex + 1}/${chunks.length} processed successfully`
        );

      } catch (error) {
        retries++;

        logger.warn(
          `Chunk ${chunkIndex + 1} failed (attempt ${retries}/${maxRetryAttempts}):`,
          error
        );

        if (retries >= maxRetryAttempts) {
          // Max retries reached - mark chunk as failed
          totalFailed += chunk.length;
          logger.error(
            `Chunk ${chunkIndex + 1} failed after ${maxRetryAttempts} attempts`
          );
        } else {
          // Exponential backoff before retry: 2^retries seconds
          const waitTimeMs = Math.pow(2, retries) * 1000;
          logger.info(`Retrying after ${waitTimeMs}ms...`);
          await new Promise(resolve => setTimeout(resolve, waitTimeMs));
        }
      }
    }
  }

  return {
    created: totalCreated,
    updated: totalUpdated,
    failed: totalFailed
  };
}

// ============================================================================
// TYPE-SAFE WRAPPERS
// ============================================================================

/**
 * Type-safe batch upsert using Prisma model names
 *
 * This wrapper provides compile-time validation of:
 * - Model names (must exist in your Prisma schema)
 * - Column names (must exist on the model)
 *
 * If you rename a model or column in Prisma schema, TypeScript will catch the error!
 *
 * @example
 * // TypeScript validates 'Consultant' exists and 'bullhornId', 'firstName' are valid columns
 * const result = await batchUpsert({
 *   prisma,
 *   model: 'Consultant',
 *   data: consultants,
 *   conflictColumn: 'bullhornId',
 *   updateColumns: ['firstName', 'lastName', 'email'],
 *   chunkSize: 100
 * });
 *
 * @example
 * // TypeScript error if you misspell or use wrong column name
 * await batchUpsert({
 *   prisma,
 *   model: 'Consultant',
 *   data: consultants,
 *   conflictColumn: 'bullhornId',
 *   updateColumns: ['fistName'] // ❌ Error: Property 'fistName' does not exist
 * });
 */
export async function batchUpsert<TModel extends PrismaModelName>(
  options: ModelUpsertOptions<TModel>
): Promise<UpsertResult> {
  return performBatchUpsert({
    prisma: options.prisma,
    tableName: options.model,
    data: options.data,
    conflictColumn: options.conflictColumn as string,
    updateColumns: options.updateColumns as string[],
    chunkSize: options.chunkSize
  });
}

/**
 * Type-safe batch upsert with retry using Prisma model names
 *
 * Same benefits as batchUpsert but with automatic retry logic
 *
 * @example
 * const result = await upsertByModelWithRetry({
 *   prisma,
 *   model: 'Consultant',
 *   data: consultants,
 *   conflictColumn: 'bullhornId',
 *   updateColumns: ['firstName', 'lastName'],
 *   chunkSize: 100,
 *   maxRetries: 3
 * });
 */
export async function upsertByModelWithRetry<TModel extends PrismaModelName>(
  options: ModelUpsertWithRetryOptions<TModel>
): Promise<UpsertWithRetryResult> {
  return batchUpsertWithRetry({
    prisma: options.prisma,
    tableName: options.model,
    data: options.data,
    conflictColumn: options.conflictColumn as string,
    updateColumns: options.updateColumns as string[],
    chunkSize: options.chunkSize,
    maxRetries: options.maxRetries
  });
}
