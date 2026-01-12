import { prisma } from '../../config/database';
import { logger } from '../../config/logger';

/**
 * Service responsible for assigning consultants to referrals
 * Uses activity history and workload to find the best match
 */
export class ConsultantAssignmentService {
  /**
   * Find the best consultant to handle a referral
   * Strategy 1: Find consultant with recent activity related to this candidate
   * Strategy 2: Find most active consultant with capacity
   */
  async findBestConsultant(candidateId: string): Promise<any> {
    const candidate = await prisma.candidate.findUnique({
      where: { id: candidateId },
    });

    if (!candidate) {
      logger.warn(`Candidate ${candidateId} not found for consultant assignment`);
      return null;
    }

    // Strategy 1: Find consultant with recent activity related to this candidate
    const activities = await prisma.consultantActivity.findMany({
      where: {
        contactBullhornId: candidate.bullhornId,
      },
      include: { consultant: true },
      orderBy: { activityDate: 'desc' },
      take: 1,
    });

    if (activities.length > 0 && activities[0].consultant.isActive) {
      logger.info(
        `Found consultant ${activities[0].consultant.firstName} ${activities[0].consultant.lastName} with previous activity for candidate ${candidate.firstName} ${candidate.lastName}`
      );
      return activities[0].consultant;
    }

    // Strategy 2: Find most active consultant with capacity
    const activeConsultant = await prisma.consultant.findFirst({
      where: { isActive: true },
      orderBy: [
        { lastActivityAt: 'desc' },
        { totalPlacements: 'desc' },
      ],
    });

    if (activeConsultant) {
      logger.info(
        `Assigned most active consultant ${activeConsultant.firstName} ${activeConsultant.lastName} for candidate ${candidate.firstName} ${candidate.lastName}`
      );
    }

    return activeConsultant;
  }

  /**
   * Find the best consultant based on workload balancing
   * Considers current referral count and success rate
   */
  async findConsultantByWorkload(): Promise<any> {
    // Find consultant with lowest active referral count
    const consultants = await prisma.consultant.findMany({
      where: { isActive: true },
      include: {
        _count: {
          select: {
            referrals: {
              where: {
                status: { in: ['PENDING', 'SENT', 'CONTACTED', 'INTERVIEWING'] },
              },
            },
          },
        },
      },
      orderBy: [
        { totalPlacements: 'desc' },
        { lastActivityAt: 'desc' },
      ],
    });

    if (consultants.length === 0) {
      logger.warn('No active consultants found for workload assignment');
      return null;
    }

    // Sort by active referral count (ascending) to balance workload
    consultants.sort((a, b) => a._count.referrals - b._count.referrals);

    const selectedConsultant = consultants[0];
    logger.info(
      `Selected consultant ${selectedConsultant.firstName} ${selectedConsultant.lastName} with ${selectedConsultant._count.referrals} active referrals`
    );

    return selectedConsultant;
  }
}

export const consultantAssignmentService = new ConsultantAssignmentService();
