import { Request, Response } from 'express';
import { dashboardService } from '../services/dashboard.service';
import { logger } from '../config/logger';

export class DashboardController {
  /**
   * Get dashboard statistics
   */
  async getStats(req: Request, res: Response): Promise<void> {
    try {
      const stats = await dashboardService.getStats();
      res.json(stats);
    } catch (error: any) {
      logger.error('Error fetching dashboard stats:', error);
      res.status(500).json({ error: error.message });
    }
  }

  /**
   * Health check endpoint
   */
  async checkHealth(req: Request, res: Response): Promise<void> {
    try {
      const health = await dashboardService.checkHealth();
      res.json(health);
    } catch (error: any) {
      logger.error('Health check failed:', error);
      res.status(503).json({
        status: 'error',
        error: error.message,
      });
    }
  }
}

export const dashboardController = new DashboardController();
