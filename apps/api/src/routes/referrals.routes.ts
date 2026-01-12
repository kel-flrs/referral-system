import { Router } from 'express';
import { referralsController } from '../controllers/referrals.controller';
import { validate } from '../middleware/validation';
import {
  getReferralsQuerySchema,
  idParamSchema,
  createReferralSchema,
  autoCreateReferralsSchema,
  sendPendingReferralsSchema,
  updateReferralStatusSchema,
} from '../schemas/validation';

const router = Router();

/**
 * GET /api/referrals
 * Get all referrals with optional filtering
 */
router.get(
  '/',
  validate(getReferralsQuerySchema, 'query'),
  (req, res) => referralsController.getReferrals(req, res)
);

/**
 * GET /api/referrals/:id
 * Get a specific referral by ID
 */
router.get(
  '/:id',
  validate(idParamSchema, 'params'),
  (req, res) => referralsController.getReferralById(req, res)
);

/**
 * POST /api/referrals
 * Create a new referral from a match
 */
router.post(
  '/',
  validate(createReferralSchema),
  (req, res) => referralsController.createReferral(req, res)
);

/**
 * POST /api/referrals/auto-create
 * Auto-create referrals for high-scoring matches
 */
router.post(
  '/auto-create',
  validate(autoCreateReferralsSchema),
  (req, res) => referralsController.autoCreateReferrals(req, res)
);

/**
 * POST /api/referrals/:id/send
 * Send a referral to Bullhorn ATS
 */
router.post(
  '/:id/send',
  validate(idParamSchema, 'params'),
  (req, res) => referralsController.sendReferral(req, res)
);

/**
 * POST /api/referrals/send-pending
 * Send all pending referrals to Bullhorn
 */
router.post(
  '/send-pending',
  validate(sendPendingReferralsSchema),
  (req, res) => referralsController.sendPendingReferrals(req, res)
);

/**
 * PATCH /api/referrals/:id
 * Update referral status
 */
router.patch(
  '/:id',
  validate(idParamSchema, 'params'),
  validate(updateReferralStatusSchema),
  (req, res) => referralsController.updateReferralStatus(req, res)
);

export default router;
