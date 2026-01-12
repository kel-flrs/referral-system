import { PrismaClient } from '@prisma/client';
import { logger } from './logger';

const prisma = new PrismaClient({
  log: [
    { level: 'query', emit: 'event' },
    { level: 'error', emit: 'stdout' },
    { level: 'warn', emit: 'stdout' },
  ],
  // Increase connection pool for better performance with batch operations
  // Default is num_physical_cpus * 2 + 1, we increase slightly for sync operations
  datasources: {
    db: {
      url: process.env.DATABASE_URL,
    },
  },
});

// Log slow queries only (no sensitive data exposure)
if (process.env.NODE_ENV === 'development') {
  prisma.$on('query' as never, (e: any) => {
    // Only log slow queries (>500ms) and omit the actual query to avoid logging sensitive data
    if (e.duration > 500) {
      logger.warn('Slow query detected:', {
        duration: `${e.duration}ms`,
        target: e.target, // Table name only, no actual query or params
      });
    }
  });
}

export { prisma };

export async function connectDatabase() {
  try {
    await prisma.$connect();
    logger.info('Connected to database');
  } catch (error) {
    logger.error('Failed to connect to database:', error);
    throw error;
  }
}

export async function disconnectDatabase() {
  await prisma.$disconnect();
  logger.info('Disconnected from database');
}
