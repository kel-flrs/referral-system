import { z } from 'zod';
import { VALID_MATCH_STATUSES, VALID_REFERRAL_STATUSES, PAGINATION, MATCHING } from '../config/constants';

// ==================== Common Schemas ====================

// Changed from CUID to UUID since database now generates UUIDs
export const cuidSchema = z.string().uuid();
export const emailSchema = z.string().email().max(255);
export const phoneSchema = z.string().regex(/^\+?[1-9]\d{9,14}$/).optional();
export const urlSchema = z.string().url().optional();

// ==================== Pagination Schemas ====================

export const paginationSchema = z.object({
  limit: z.coerce.number().int().min(1).max(PAGINATION.MAX_LIMIT).default(PAGINATION.DEFAULT_LIMIT),
  offset: z.coerce.number().int().min(0).default(PAGINATION.DEFAULT_OFFSET),
});

// ==================== Match Schemas ====================

export const matchStatusSchema = z.enum(VALID_MATCH_STATUSES as [string, ...string[]]);

export const updateMatchStatusSchema = z.object({
  status: matchStatusSchema,
});

export const getMatchesQuerySchema = z.object({
  status: matchStatusSchema.optional(),
  minScore: z.coerce.number().int().min(0).max(100).optional(),
  positionId: cuidSchema.optional(),
  candidateId: cuidSchema.optional(),
  limit: z.coerce.number().int().min(1).max(PAGINATION.MAX_LIMIT).default(PAGINATION.DEFAULT_LIMIT),
  offset: z.coerce.number().int().min(0).default(PAGINATION.DEFAULT_OFFSET),
});

// ==================== Referral Schemas ====================

export const referralStatusSchema = z.enum(VALID_REFERRAL_STATUSES as [string, ...string[]]);

export const createReferralSchema = z.object({
  matchId: cuidSchema,
  consultantId: cuidSchema.optional(),
  referralSource: z.string().min(1).max(255).default('Consultant dashboard'),
  referrerName: z.string().max(100).optional(),
  referrerEmail: emailSchema.optional(),
  referrerPhone: phoneSchema,
  referrerLinkedIn: urlSchema,
  notes: z.string().max(1000).optional(),
});

export const updateReferralStatusSchema = z.object({
  status: referralStatusSchema,
});

export const autoCreateReferralsSchema = z.object({
  minScore: z.number().int().min(0).max(100).default(MATCHING.SCORE.AUTO_REFERRAL_THRESHOLD),
});

export const sendPendingReferralsSchema = z.object({
  limit: z.number().int().min(1).max(100).default(10),
});

export const getReferralsQuerySchema = z.object({
  status: referralStatusSchema.optional(),
  consultantId: cuidSchema.optional(),
  positionId: cuidSchema.optional(),
  candidateId: cuidSchema.optional(),
  limit: z.coerce.number().int().min(1).max(PAGINATION.MAX_LIMIT).default(PAGINATION.DEFAULT_LIMIT),
  offset: z.coerce.number().int().min(0).default(PAGINATION.DEFAULT_OFFSET),
});

// ==================== Sync Schemas ====================

export const syncCandidatesSchema = z.object({
  modifiedSince: z.string().datetime().optional(),
});

// ==================== ID Param Schema ====================

export const idParamSchema = z.object({
  id: cuidSchema,
});
