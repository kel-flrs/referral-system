import express, { Application } from 'express';
import cors from 'cors';
import helmet from 'helmet';
import rateLimit from 'express-rate-limit';
import { logger } from './config/logger';
import { correlationId } from './middleware/correlation';

// Import routes
import matchesRoutes from './routes/matches.routes';
import referralsRoutes from './routes/referrals.routes';
import dashboardRoutes from './routes/dashboard.routes';

export function createApp(): Application {
  const app = express();

  // Security middleware
  app.use(helmet());
  app.use(cors());

  // Rate limiting
  const limiter = rateLimit({
    windowMs: 15 * 60 * 1000, // 15 minutes
    max: 100, // Limit each IP to 100 requests per windowMs
    message: 'Too many requests from this IP, please try again later',
  });
  app.use('/api/', limiter);

  // Body parsing
  app.use(express.json());
  app.use(express.urlencoded({ extended: true }));

  // Correlation ID for request tracking
  app.use(correlationId);

  // Request logging
  app.use((req, res, next) => {
    logger.info(`${req.method} ${req.path}`, {
      correlationId: (req as any).correlationId,
      ip: req.ip,
      userAgent: req.get('user-agent'),
    });
    next();
  });

  // Routes
  app.use('/api/matches', matchesRoutes);
  app.use('/api/referrals', referralsRoutes);
  app.use('/api/dashboard', dashboardRoutes);

  // Root endpoint
  app.get('/', (req, res) => {
    res.json({
      name: 'Referral System API',
      version: '1.0.0',
      status: 'running',
      endpoints: {
        health: '/api/dashboard/health',
        stats: '/api/dashboard/stats',
        matches: '/api/matches',
        referrals: '/api/referrals',
      },
    });
  });

  // NOTE: Error handlers are registered in index.ts after Bull Board is mounted

  return app;
}
