import { Router } from 'express';
import { dashboardController } from '../controllers/dashboard.controller';

const router = Router();

/**
 * GET /api/dashboard/stats
 * Get dashboard statistics
 */
router.get('/stats', (req, res) => dashboardController.getStats(req, res));

/**
 * GET /api/dashboard/health
 * Health check endpoint
 */
router.get('/health', (req, res) => dashboardController.checkHealth(req, res));

export default router;
