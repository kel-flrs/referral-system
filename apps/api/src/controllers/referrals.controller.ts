import { Request, Response } from 'express';
import { referralService } from '../services/referral/referral.service';
import { logger } from '../config/logger';

export class ReferralsController {
  /**
   * Get all referrals with optional filtering
   */
  async getReferrals(req: Request, res: Response): Promise<void> {
    try {
      const {
        status,
        consultantId,
        positionId,
        limit = '50',
        offset = '0',
      } = req.query;

      const { referrals, total } = await referralService.getReferrals({
        status: status as string,
        consultantId: consultantId as string,
        positionId: positionId as string,
        limit: parseInt(limit as string),
        offset: parseInt(offset as string),
      });

      res.json({
        data: referrals,
        pagination: {
          total,
          limit: parseInt(limit as string),
          offset: parseInt(offset as string),
        },
      });
    } catch (error: any) {
      logger.error('Error fetching referrals:', error);
      res.status(500).json({ error: error.message });
    }
  }

  /**
   * Get a specific referral by ID
   */
  async getReferralById(req: Request, res: Response): Promise<void> {
    try {
      const { id } = req.params;
      const referral = await referralService.getReferralById(id);

      if (!referral) {
        res.status(404).json({ error: 'Referral not found' });
        return;
      }

      res.json({ data: referral });
    } catch (error: any) {
      logger.error('Error fetching referral:', error);
      res.status(500).json({ error: error.message });
    }
  }

  /**
   * Create a new referral from a match
   */
  async createReferral(req: Request, res: Response): Promise<void> {
    try {
      const {
        matchId,
        consultantId,
        referralSource,
        referrerName,
        referrerEmail,
        referrerPhone,
        notes,
      } = req.body;

      const referral = await referralService.createReferralWithDetails(
        matchId,
        consultantId,
        referralSource,
        {
          name: referrerName,
          email: referrerEmail,
          phone: referrerPhone,
        },
        notes
      );

      res.status(201).json({ data: referral });
    } catch (error: any) {
      if (error.message.includes('required')) {
        res.status(400).json({ error: error.message });
        return;
      }
      logger.error('Error creating referral:', error);
      res.status(500).json({ error: error.message });
    }
  }

  /**
   * Auto-create referrals for high-scoring matches
   */
  async autoCreateReferrals(req: Request, res: Response): Promise<void> {
    try {
      const { minScore = 85 } = req.body;
      await referralService.autoCreateReferrals(minScore);
      res.json({ data: { message: 'Auto-create referrals completed' } });
    } catch (error: any) {
      logger.error('Error auto-creating referrals:', error);
      res.status(500).json({ error: error.message });
    }
  }

  /**
   * Send a referral to Bullhorn ATS
   */
  async sendReferral(req: Request, res: Response): Promise<void> {
    try {
      const { id } = req.params;
      const referral = await referralService.sendReferralAndReturn(id);

      res.json({ data: referral });
    } catch (error: any) {
      logger.error('Error sending referral:', error);
      res.status(500).json({ error: error.message });
    }
  }

  /**
   * Send all pending referrals (handled by pipelines)
   */
  async sendPendingReferrals(req: Request, res: Response): Promise<void> {
    res.status(501).json({ message: 'Referral sending is handled by data pipelines' });
  }

  /**
   * Update referral status
   */
  async updateReferralStatus(req: Request, res: Response): Promise<void> {
    try {
      const { id } = req.params;
      const { status } = req.body;

      const referral = await referralService.updateReferralStatus(id, status);

      res.json({ data: referral });
    } catch (error: any) {
      if (error.message === 'Invalid status') {
        res.status(400).json({ error: error.message });
        return;
      }
      logger.error('Error updating referral:', error);
      res.status(500).json({ error: error.message });
    }
  }
}

export const referralsController = new ReferralsController();
