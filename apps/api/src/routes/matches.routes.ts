import { Router } from 'express';
import { matchesController } from '../controllers/matches.controller';
import { validate } from '../middleware/validation';
import {
  getMatchesQuerySchema,
  idParamSchema,
  updateMatchStatusSchema,
} from '../schemas/validation';

const router = Router();

/**
 * GET /api/matches
 * Get all matches with optional filtering
 */
router.get(
  '/',
  validate(getMatchesQuerySchema, 'query'),
  (req, res) => matchesController.getMatches(req, res)
);


/**
 * GET /api/matches/:id
 * Get a specific match by ID
 */
router.get(
  '/:id',
  validate(idParamSchema, 'params'),
  (req, res) => matchesController.getMatchById(req, res)
);

/**
 * GET /api/matches/positions/:positionId/similar-candidates
 * Find similar candidates for a position using semantic matching
 */
router.get(
  '/positions/:positionId/similar-candidates',
  (req, res) => matchesController.findSimilarCandidates(req, res)
);

/**
 * PATCH /api/matches/:id
 * Update match status
 */
router.patch(
  '/:id',
  validate(idParamSchema, 'params'),
  validate(updateMatchStatusSchema),
  (req, res) => matchesController.updateMatchStatus(req, res)
);

export default router;
