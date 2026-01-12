import 'dotenv/config';
import { createApp } from './app';
import { connectDatabase, disconnectDatabase } from './config/database';
import { logger } from './config/logger';
import { errorHandler, notFoundHandler } from './middleware/errorHandler';

const PORT = process.env.PORT || 3222;

async function start() {
  try {
    // Connect to database
    await connectDatabase();

    // Create Express app
    const app = createApp();

    // Register error handlers
    app.use(notFoundHandler);
    app.use(errorHandler);

    // Start server
    const server = app.listen(PORT, () => {
      logger.info(`Server running on port ${PORT}`);
      logger.info(`Environment: ${process.env.NODE_ENV || 'development'}`);
    });

    // Graceful shutdown
    const shutdown = async () => {
      logger.info('Shutting down gracefully...');

      server.close(() => {
        logger.info('HTTP server closed');
      });

      await disconnectDatabase();
      process.exit(0);
    };

    process.on('SIGTERM', shutdown);
    process.on('SIGINT', shutdown);

    process.on('uncaughtException', (error) => {
      logger.error('Uncaught exception:', error);
      shutdown();
    });

    process.on('unhandledRejection', (reason, promise) => {
      logger.error('Unhandled rejection at:', promise, 'reason:', reason);
      shutdown();
    });
  } catch (error) {
    logger.error('Failed to start server:', error);
    process.exit(1);
  }
}

start();
