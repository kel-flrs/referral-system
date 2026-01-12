import type { Prisma } from '@prisma/client';
import { prisma } from '../config/database';

export type GetMatchesParams = {
  status?: string;
  minScore?: number;
  positionId?: string;
  candidateId?: string;
  limit: number;
  offset: number;
};

export type FindSimilarCandidatesParams = {
  limit: number;
  threshold: number;
};

export class BadRequestError extends Error {
  readonly statusCode = 400;
}
export class NotFoundError extends Error {
  readonly statusCode = 404;
}

export class MatchesService {

  async getMatches(params: GetMatchesParams) {
    const { status, minScore, positionId, candidateId, limit, offset } = params;

    const where: Prisma.MatchWhereInput = {};
    if (status) where.status = status as any;
    if (typeof minScore === 'number' && Number.isFinite(minScore)) {
      where.overallScore = { gte: minScore };
    }
    if (positionId) where.positionId = positionId;
    if (candidateId) where.candidateId = candidateId;

    const [matches, total] = await Promise.all([
      prisma.match.findMany({
        where,
        include: {
          candidate: {
            select: {
              id: true,
              firstName: true,
              lastName: true,
              email: true,
              currentTitle: true,
              location: true,
            },
          },
          position: {
            select: {
              id: true,
              title: true,
              clientName: true,
              location: true,
              status: true,
            },
          },
        },
        orderBy: { overallScore: 'desc' },
        take: limit,
        skip: offset,
      }),
      prisma.match.count({ where }),
    ]);

    return {
      data: matches,
      pagination: { total, limit, offset },
    };
  }

  async getMatchById(id: string) {
    const match = await prisma.match.findUnique({
      where: { id },
      include: {
        candidate: true,
        position: true,
        referral: true,
      },
    });

    if (!match) throw new NotFoundError('Match not found');
    return { data: match };
  }

  async findSimilarCandidates(positionId: string, params: FindSimilarCandidatesParams) {
    const { limit, threshold } = params;

    const candidates = await prisma.$queryRaw<any[]>`
      SELECT
        c.id,
        c."bullhornId",
        c."firstName",
        c."lastName",
        c.email,
        c.skills,
        c."currentTitle",
        (1 - (c."profileEmbedding" <=> p."descriptionEmbedding")) as semantic_score
      FROM "Candidate" c
      CROSS JOIN "Position" p
      WHERE p.id = ${positionId}
        AND c."profileEmbedding" IS NOT NULL
        AND p."descriptionEmbedding" IS NOT NULL
        AND (1 - (c."profileEmbedding" <=> p."descriptionEmbedding")) >= ${threshold}
      ORDER BY c."profileEmbedding" <=> p."descriptionEmbedding"
      LIMIT ${limit}
    `;

    return { data: candidates, count: candidates.length };
  }

  async updateMatchStatus(id: string, status: string) {
    const validStatuses = ['PENDING', 'REVIEWED', 'REFERRED', 'REJECTED'] as const;
    if (!validStatuses.includes(status as any)) {
      throw new BadRequestError('Invalid status');
    }

    const match = await prisma.match.update({
      where: { id },
      data: { status: status as any },
    });

    return { data: match };
  }
}

export const matchesService = new MatchesService();
