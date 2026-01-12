import { prisma } from '../config/database';
import { logger } from '../config/logger';

export class DashboardService {
  /**
   * Get dashboard statistics
   */
  async getStats(): Promise<any> {
    const [
      totalConsultants,
      activeConsultants,
      totalCandidates,
      activeCandidates,
      totalPositions,
      openPositions,
      totalMatches,
      pendingMatches,
      totalReferrals,
      pendingReferrals,
      sentReferrals,
    ] = await Promise.all([
      prisma.consultant.count(),
      prisma.consultant.count({ where: { isActive: true } }),
      prisma.candidate.count(),
      prisma.candidate.count({ where: { status: 'ACTIVE' } }),
      prisma.position.count(),
      prisma.position.count({ where: { status: 'OPEN' } }),
      prisma.match.count(),
      prisma.match.count({ where: { status: 'PENDING' } }),
      prisma.referral.count(),
      prisma.referral.count({ where: { status: 'PENDING' } }),
      prisma.referral.count({ where: { status: 'SENT' } }),
    ]);

    // Get top consultants by referrals
    const topConsultants = await prisma.consultant.findMany({
      where: { isActive: true },
      orderBy: { totalReferrals: 'desc' },
      take: 5,
      select: {
        id: true,
        firstName: true,
        lastName: true,
        totalReferrals: true,
        totalPlacements: true,
      },
    });

    // Get recent high-scoring matches
    const recentMatches = await prisma.match.findMany({
      where: {
        status: 'PENDING',
        overallScore: { gte: 80 },
      },
      include: {
        candidate: {
          select: {
            firstName: true,
            lastName: true,
            currentTitle: true,
          },
        },
        position: {
          select: {
            title: true,
            clientName: true,
          },
        },
      },
      orderBy: [{ overallScore: 'desc' }, { createdAt: 'desc' }],
      take: 10,
    });

    return {
      overview: {
        consultants: {
          total: totalConsultants,
          active: activeConsultants,
        },
        candidates: {
          total: totalCandidates,
          active: activeCandidates,
        },
        positions: {
          total: totalPositions,
          open: openPositions,
        },
        matches: {
          total: totalMatches,
          pending: pendingMatches,
        },
        referrals: {
          total: totalReferrals,
          pending: pendingReferrals,
          sent: sentReferrals,
        },
      },
      topConsultants,
      recentMatches,
    };
  }

  /**
   * Perform health check
   */
  async checkHealth(): Promise<{
    status: string;
    database: string;
    timestamp: string;
  }> {
    try {
      // Check database connection
      await prisma.$queryRaw`SELECT 1`;

      return {
        status: 'ok',
        database: 'connected',
        timestamp: new Date().toISOString(),
      };
    } catch (error) {
      logger.error('Health check failed:', error);
      throw error;
    }
  }
}

export const dashboardService = new DashboardService();
