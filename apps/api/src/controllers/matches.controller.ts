import { Request, Response } from 'express';
import { matchesService } from '../services/matches.service';
import { BadRequestError, NotFoundError } from '@referral-system/shared';
import { Prisma } from '@referral-system/database';

const toInt = (v: unknown, fallback: number) => {
  const n = typeof v === 'string' ? parseInt(v, 10) : Number(v);
  return Number.isFinite(n) ? n : fallback;
};

const toFloat = (v: unknown) => {
  const n = typeof v === 'string' ? parseFloat(v) : Number(v);
  return Number.isFinite(n) ? n : undefined;
};

export class MatchesController {

  async getMatches(req: Request, res: Response): Promise<void> {
    try {
      const { status, minScore, positionId, candidateId } = req.query;

      const limit = toInt(req.query.limit, 50);
      const offset = toInt(req.query.offset, 0);

      const result = await matchesService.getMatches({
        status: status as string | undefined,
        minScore: toFloat(minScore),
        positionId: positionId as string | undefined,
        candidateId: candidateId as string | undefined,
        limit,
        offset,
      });

      res.json(result);
    } catch (error: any) {
      res.status(500).json({ error: error.message });
    }
  }

  async getMatchById(req: Request, res: Response): Promise<void> {
    try {
      const { id } = req.params;
      const result = await matchesService.getMatchById(id);
      res.json(result);
    } catch (error: any) {
      if (error instanceof NotFoundError) {
        res.status(error.statusCode).json({ error: error.message });
        return;
      }
      res.status(500).json({ error: error.message });
    }
  }

  async findSimilarCandidates(req: Request, res: Response): Promise<void> {
    try {
      const { positionId } = req.params;

      const limit = toInt(req.query.limit, 20);
      const threshold = toFloat(req.query.threshold) ?? 0.7;

      const result = await matchesService.findSimilarCandidates(positionId, {
        limit,
        threshold,
      });

      res.json(result);
    } catch (error: any) {
      res.status(500).json({ error: error.message });
    }
  }

  async updateMatchStatus(req: Request, res: Response): Promise<void> {
    try {
      const { id } = req.params;
      const { status } = req.body as { status?: string };

      if (!status) {
        res.status(400).json({ error: 'status is required' });
        return;
      }

      const result = await matchesService.updateMatchStatus(id, status);
      res.json(result);
    } catch (error: any) {
      if (error instanceof BadRequestError) {
        res.status(error.statusCode).json({ error: error.message });
        return;
      }

      if (error instanceof Prisma.PrismaClientKnownRequestError && error.code === 'P2025') {
        res.status(404).json({ error: 'Match not found' });
        return;
      }

      res.status(500).json({ error: error.message });
    }
  }
}

export const matchesController = new MatchesController();
