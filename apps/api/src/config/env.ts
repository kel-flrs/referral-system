import { z } from 'zod';
import { logger } from './logger';

const envSchema = z.object({
  // Server
  NODE_ENV: z.enum(['development', 'production', 'test']).default('development'),
  PORT: z.coerce.number().int().min(1).max(65535).default(3000),

  // Database
  DATABASE_URL: z.string().url(),

  // Redis
  REDIS_HOST: z.string().default('localhost'),
  REDIS_PORT: z.coerce.number().int().min(1).max(65535).default(6379),
  REDIS_PASSWORD: z.string().optional(),

  // Bullhorn (optional in mock mode)
  BULLHORN_MOCK_MODE: z.string().default('true'),

  // Mock Bullhorn service
  BULLHORN_MOCK_BASE_URL: z.string().url().optional(),
  BULLHORN_MOCK_CLIENT_ID: z.string().optional(),
  BULLHORN_MOCK_CLIENT_SECRET: z.string().optional(),
  BULLHORN_MOCK_USERNAME: z.string().optional(),
  BULLHORN_MOCK_PASSWORD: z.string().optional(),

  // Real Bullhorn service
  BULLHORN_CLIENT_ID: z.string().optional(),
  BULLHORN_CLIENT_SECRET: z.string().optional(),
  BULLHORN_USERNAME: z.string().optional(),
  BULLHORN_PASSWORD: z.string().optional(),
  BULLHORN_TOKEN_URL: z.string().url().optional(),
  BULLHORN_LOGIN_URL: z.string().url().optional(),

  // Job Queue
  SYNC_INTERVAL_MINUTES: z.coerce.number().int().min(1).default(30),
  MATCHING_SCORE_THRESHOLD: z.coerce.number().int().min(0).max(100).default(70),

  // Logging
  LOG_LEVEL: z.enum(['error', 'warn', 'info', 'debug']).default('info'),
});

/**
 * Validate environment variables at startup
 * @throws {Error} if validation fails with detailed error message
 */
export function validateEnv() {
  const result = envSchema.safeParse(process.env);

  if (!result.success) {
    const errors = result.error.errors.map(err =>
      `  - ${err.path.join('.')}: ${err.message}`
    ).join('\n');

    const errorMessage = `Environment validation failed:\n${errors}`;
    logger.error(errorMessage);
    throw new Error(errorMessage);
  }

  // If in mock mode, validate mock service credentials are provided
  if (result.data.BULLHORN_MOCK_MODE === 'true') {
    const requiredMockVars = [
      'BULLHORN_MOCK_BASE_URL',
      'BULLHORN_MOCK_CLIENT_ID',
      'BULLHORN_MOCK_CLIENT_SECRET',
      'BULLHORN_MOCK_USERNAME',
      'BULLHORN_MOCK_PASSWORD',
    ];

    const missingVars = requiredMockVars.filter(
      varName => !result.data[varName as keyof typeof result.data]
    );

    if (missingVars.length > 0) {
      const errorMessage = `Mock Bullhorn service credentials required when BULLHORN_MOCK_MODE=true. Missing: ${missingVars.join(', ')}`;
      logger.error(errorMessage);
      throw new Error(errorMessage);
    }
  }

  // If not in mock mode, validate real Bullhorn credentials are provided
  if (result.data.BULLHORN_MOCK_MODE !== 'true') {
    const requiredBullhornVars = [
      'BULLHORN_CLIENT_ID',
      'BULLHORN_CLIENT_SECRET',
      'BULLHORN_USERNAME',
      'BULLHORN_PASSWORD',
      'BULLHORN_TOKEN_URL',
      'BULLHORN_LOGIN_URL',
    ];

    const missingVars = requiredBullhornVars.filter(
      varName => !result.data[varName as keyof typeof result.data]
    );

    if (missingVars.length > 0) {
      const errorMessage = `Bullhorn credentials required when BULLHORN_MOCK_MODE=false. Missing: ${missingVars.join(', ')}`;
      logger.error(errorMessage);
      throw new Error(errorMessage);
    }
  }

  logger.info('Environment variables validated successfully');
  return result.data;
}

// Export validated environment
export const env = validateEnv();
