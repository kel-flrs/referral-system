import { prisma } from '../../config/database';
import { logger } from '../../config/logger';

/**
 * Service responsible for referral status updates.
 * Note: Actual Bullhorn ATS integration is handled by data pipelines.
 */
export class ReferralBullhornService {
  /**
   * Mark a referral as ready to send (actual sync handled by pipelines)
   */
  async sendReferralToBullhorn(referralId: string): Promise<void> {
    const referral = await prisma.referral.findUnique({
      where: { id: referralId },
    });

    if (!referral) {
      throw new Error(`Referral ${referralId} not found`);
    }

    if (referral.sentToBullhornAt) {
      logger.warn(`Referral ${referralId} already marked as sent`);
      return;
    }

    // Mark as SENT - actual Bullhorn sync handled by pipelines
    await prisma.referral.update({
      where: { id: referralId },
      data: {
        status: 'SENT',
        sentToBullhornAt: new Date(),
      },
    });

    logger.info(`Marked referral ${referralId} as sent (pipeline will sync to Bullhorn)`);
  }

  /**
   * Mark multiple pending referrals as sent (actual sync handled by pipelines)
   */
  async sendPendingReferrals(limit: number = 10): Promise<{ sent: number; failed: number }> {
    const pendingReferrals = await prisma.referral.findMany({
      where: {
        status: 'PENDING',
        sentToBullhornAt: null,
      },
      take: limit,
    });

    logger.info(`Marking ${pendingReferrals.length} pending referrals as sent...`);

    let sent = 0;
    let failed = 0;

    for (const referral of pendingReferrals) {
      try {
        await this.sendReferralToBullhorn(referral.id);
        sent++;
      } catch (error) {
        failed++;
        logger.error(`Failed to mark referral ${referral.id}:`, error);
      }
    }

    logger.info(`Marked ${sent} referrals as sent, ${failed} failed`);

    return { sent, failed };
  }
}

export const referralBullhornService = new ReferralBullhornService();
