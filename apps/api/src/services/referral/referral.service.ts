import { prisma } from '../../config/database';
import { logger } from '../../config/logger';
import { consultantAssignmentService } from './consultant-assignment.service';
import { referralBullhornService } from './referral-bullhorn.service';

export class ReferralService {

  async createReferral(
    matchId: string,
    consultantId: string | undefined,
    referralSource: string,
    referrerInfo?: {
      name?: string;
      email?: string;
      phone?: string;
    },
    notes?: string
  ): Promise<string> {
    const match = await prisma.match.findUnique({
      where: { id: matchId },
      include: {
        candidate: true,
        position: true,
      },
    });

    if (!match) {
      throw new Error(`Match ${matchId} not found`);
    }

    if (match.status === 'REFERRED') {
      throw new Error(`Match ${matchId} has already been referred`);
    }

    const resolvedConsultantId = consultantId
      ?? (await consultantAssignmentService.findBestConsultant(match.candidateId))?.id
      ?? (await consultantAssignmentService.findConsultantByWorkload())?.id;

    if (!resolvedConsultantId) {
      throw new Error('No active consultant available to assign this referral');
    }

    const referral = await prisma.$transaction(async (tx) => {
      const newReferral = await tx.referral.create({
        data: {
          matchId: match.id,
          candidateId: match.candidateId,
          positionId: match.positionId,
          consultantId: resolvedConsultantId,
          referralSource,
          referrerName: referrerInfo?.name || null,
          referrerEmail: referrerInfo?.email || null,
          referrerPhone: referrerInfo?.phone || null,
          notes: notes || null,
          status: 'PENDING',
        },
      });

      await tx.match.update({
        where: { id: matchId },
        data: { status: 'REFERRED' },
      });

      await tx.consultant.update({
        where: { id: resolvedConsultantId },
        data: {
          totalReferrals: { increment: 1 },
        },
      });

      return newReferral;
    });

    logger.info(
      `Created referral ${referral.id}: ${match.candidate.firstName} ${match.candidate.lastName} -> ${match.position.title}`
    );

    return referral.id;
  }

  async createReferralWithDetails(
    matchId: string,
    consultantId: string | undefined,
    referralSource: string,
    referrerInfo?: {
      name?: string;
      email?: string;
      phone?: string;
    },
    notes?: string
  ): Promise<any> {
    if (!matchId || !referralSource) {
      throw new Error('matchId and referralSource are required');
    }

    const referralId = await this.createReferral(
      matchId,
      consultantId,
      referralSource,
      referrerInfo,
      notes
    );

    const referral = await prisma.referral.findUnique({
      where: { id: referralId },
      include: {
        candidate: true,
        position: true,
        consultant: true,
      },
    });

    return referral;
  }

  async autoCreateReferrals(minScore: number = 85): Promise<void> {
    logger.info(`Auto-creating referrals for matches with score >= ${minScore}...`);

    const topMatches = await prisma.match.findMany({
      where: {
        status: 'PENDING',
        overallScore: { gte: minScore },
      },
      include: {
        candidate: true,
        position: true,
      },
      orderBy: {
        overallScore: 'desc',
      },
      take: 100,
    });

    logger.info(`Found ${topMatches.length} high-scoring matches`);

    let created = 0;

    for (const match of topMatches) {
      const consultant = await consultantAssignmentService.findBestConsultant(match.candidateId);

      if (!consultant) {
        logger.warn(`No consultant found for match ${match.id}`);
        continue;
      }

      const connections = await prisma.candidateConnection.findMany({
        where: { candidateId: match.candidateId },
        orderBy: { relationshipStrength: 'desc' },
        take: 1,
      });

      const referralSource = connections.length > 0
        ? `Network connection: ${connections[0].connectionType}`
        : 'AI-based matching';

      const referrerInfo = connections.length > 0
        ? {
            name: connections[0].connectedName,
            email: connections[0].connectedEmail || undefined,
          }
        : undefined;

      try {
        await this.createReferral(
          match.id,
          consultant.id,
          referralSource,
          referrerInfo,
          `Auto-generated referral based on ${match.overallScore.toFixed(1)}% match score. ${match.matchReason}`
        );
        created++;
      } catch (error) {
        logger.error(`Failed to create referral for match ${match.id}:`, error);
      }
    }

    logger.info(`Auto-created ${created} referrals`);
  }

  async getTopMatches(limit: number = 50, minScore: number = 70): Promise<any[]> {
    const matches = await prisma.match.findMany({
      where: {
        status: 'PENDING',
        overallScore: { gte: minScore },
      },
      include: {
        candidate: true,
        position: true,
      },
      orderBy: {
        overallScore: 'desc',
      },
      take: limit,
    });

    return matches;
  }

  async getReferrals(filters: {
    status?: string;
    consultantId?: string;
    positionId?: string;
    limit?: number;
    offset?: number;
  }): Promise<{ referrals: any[]; total: number }> {
    const {
      status,
      consultantId,
      positionId,
      limit = 50,
      offset = 0,
    } = filters;

    const where: any = {};

    if (status) where.status = status;
    if (consultantId) where.consultantId = consultantId;
    if (positionId) where.positionId = positionId;

    const [referrals, total] = await Promise.all([
      prisma.referral.findMany({
        where,
        include: {
          candidate: {
            select: {
              id: true,
              firstName: true,
              lastName: true,
              email: true,
              phone: true,
              currentTitle: true,
            },
          },
          position: {
            select: {
              id: true,
              title: true,
              clientName: true,
            },
          },
          consultant: {
            select: {
              id: true,
              firstName: true,
              lastName: true,
              email: true,
            },
          },
          match: {
            select: {
              overallScore: true,
              matchReason: true,
            },
          },
        },
        orderBy: { createdAt: 'desc' },
        take: limit,
        skip: offset,
      }),
      prisma.referral.count({ where }),
    ]);

    return { referrals, total };
  }

  async getReferralById(id: string): Promise<any | null> {
    const referral = await prisma.referral.findUnique({
      where: { id },
      include: {
        candidate: true,
        position: true,
        consultant: true,
        match: true,
      },
    });

    return referral;
  }

  async updateReferralStatus(id: string, status: string): Promise<any> {
    const validStatuses = [
      'PENDING',
      'SENT',
      'CONTACTED',
      'INTERVIEWING',
      'REJECTED',
      'PLACED',
    ];

    if (!validStatuses.includes(status)) {
      throw new Error('Invalid status');
    }

    const referral = await prisma.referral.update({
      where: { id },
      data: { status },
    });

    logger.info(`Updated referral ${id} status to ${status}`);

    return referral;
  }

  async sendReferralToBullhorn(referralId: string): Promise<void> {
    await referralBullhornService.sendReferralToBullhorn(referralId);
  }

  async sendReferralAndReturn(id: string): Promise<any> {
    await referralBullhornService.sendReferralToBullhorn(id);

    const referral = await prisma.referral.findUnique({
      where: { id },
    });

    return referral;
  }

  async sendPendingReferrals(limit: number = 10): Promise<void> {
    await referralBullhornService.sendPendingReferrals(limit);
  }
}

export const referralService = new ReferralService();
