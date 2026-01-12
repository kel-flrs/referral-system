// ==================== Server Configuration ====================
export const SERVER = {
  DEFAULT_PORT: 3000,
  DEFAULT_ENV: 'development',
} as const;

// ==================== Database Configuration ====================
export const DATABASE = {
  PORTS: {
    POSTGRES: 5432,
    REDIS: 6379,
    PGADMIN: 5050,
    PRISMA_STUDIO: 5555,
  },
} as const;

// ==================== Pagination ====================
export const PAGINATION = {
  DEFAULT_LIMIT: 50,
  DEFAULT_OFFSET: 0,
  MAX_LIMIT: 1000,
} as const;

// ==================== Job Queue Configuration ====================
export const JOBS = {
  RETRY: {
    MAX_ATTEMPTS: 5,
    BACKOFF_DELAY_MS: 10000, // 10 seconds base delay for rate-limited APIs
  },
  RETENTION: {
    COMPLETED: 100,
    FAILED: 500,
  },
  CONCURRENCY: 2,
} as const;

// ==================== Job Names ====================
export const JOB_NAMES = {
  // Recurring jobs
  SYNC_CONSULTANTS_RECURRING: 'sync-consultants-recurring',
  SYNC_CANDIDATES_RECURRING: 'sync-candidates-recurring',
  SYNC_POSITIONS_RECURRING: 'sync-positions-recurring',
  FIND_MATCHES_RECURRING: 'find-matches-recurring',
  AUTO_CREATE_REFERRALS_RECURRING: 'auto-create-referrals-recurring',
  SEND_REFERRALS_RECURRING: 'send-referrals-recurring',

  // Manual jobs
  SYNC_CONSULTANTS_MANUAL: 'sync-consultants-manual',
  SYNC_CANDIDATES_MANUAL: 'sync-candidates-manual',
  SYNC_POSITIONS_MANUAL: 'sync-positions-manual',
  FIND_MATCHES_MANUAL: 'find-matches-manual',
  AUTO_CREATE_REFERRALS_MANUAL: 'auto-create-referrals-manual',
  SEND_REFERRALS_MANUAL: 'send-referrals-manual',
} as const;

// ==================== Sync Configuration ====================
export const SYNC = {
  INTERVAL_MINUTES: 30,
  MATCHING_DELAY_MINUTES: 5,
  REFERRAL_SEND_INTERVAL_MINUTES: 15,
  AUTO_REFERRAL_INTERVAL_MINUTES: 60,
  BULLHORN_QUERY_LIMIT: 500,
} as const;

// ==================== Matching Algorithm ====================
export const MATCHING = {
  SCORE: {
    MIN_THRESHOLD: 70,
    AUTO_REFERRAL_THRESHOLD: 85,
    MAX_SCORE: 100,
  },
  WEIGHTS: {
    SKILL_MATCH: 0.5,
    EXPERIENCE: 0.3,
    LOCATION: 0.2,
  },
  LOCATION_SCORES: {
    PERFECT: 100,
    SAME_CITY: 90,
    SAME_STATE: 70,
    REMOTE: 100,
    UNKNOWN: 50,
    DIFFERENT: 30,
  },
  EXPERIENCE_SCORES: {
    PERFECT_MATCH: 100,
    ONE_LEVEL_OFF: 75,
    TWO_LEVELS_OFF: 50,
    MORE_OFF: 25,
  },
} as const;

// ==================== Experience Levels ====================
export const EXPERIENCE_LEVELS = {
  ENTRY: 1,
  JUNIOR: 1,
  MID: 2,
  SENIOR: 3,
  LEAD: 4,
  PRINCIPAL: 4,
  EXECUTIVE: 5,
} as const;

export type ExperienceLevel = keyof typeof EXPERIENCE_LEVELS;

// ==================== Status Enums ====================
export const STATUS = {
  MATCH: {
    PENDING: 'PENDING',
    REVIEWED: 'REVIEWED',
    REFERRED: 'REFERRED',
    REJECTED: 'REJECTED',
  },
  REFERRAL: {
    PENDING: 'PENDING',
    SENT: 'SENT',
    CONTACTED: 'CONTACTED',
    INTERVIEWING: 'INTERVIEWING',
    REJECTED: 'REJECTED',
    PLACED: 'PLACED',
  },
  CANDIDATE: {
    ACTIVE: 'ACTIVE',
    PLACED: 'PLACED',
    INACTIVE: 'INACTIVE',
  },
  POSITION: {
    OPEN: 'OPEN',
    FILLED: 'FILLED',
    CLOSED: 'CLOSED',
  },
  JOB: {
    PENDING: 'PENDING',
    PROCESSING: 'PROCESSING',
    COMPLETED: 'COMPLETED',
    FAILED: 'FAILED',
  },
} as const;

// Type helpers for status values
export type MatchStatus = typeof STATUS.MATCH[keyof typeof STATUS.MATCH];
export type ReferralStatus = typeof STATUS.REFERRAL[keyof typeof STATUS.REFERRAL];
export type CandidateStatus = typeof STATUS.CANDIDATE[keyof typeof STATUS.CANDIDATE];
export type PositionStatus = typeof STATUS.POSITION[keyof typeof STATUS.POSITION];

// Arrays for validation
export const VALID_MATCH_STATUSES = Object.values(STATUS.MATCH);
export const VALID_REFERRAL_STATUSES = Object.values(STATUS.REFERRAL);

// ==================== Connection Types ====================
export const CONNECTION_TYPES = {
  LINKEDIN: 'LINKEDIN',
  COLLEAGUE: 'COLLEAGUE',
  REFERRAL: 'REFERRAL',
  MANUAL: 'MANUAL',
} as const;

// ==================== Activity Types ====================
export const ACTIVITY_TYPES = {
  CALL: 'CALL',
  EMAIL: 'EMAIL',
  MEETING: 'MEETING',
  NOTE: 'NOTE',
} as const;

// ==================== Referral Configuration ====================
export const REFERRAL = {
  DEFAULT_LIMIT: 10,
  MAX_PENDING_TO_SEND: 20,
  AUTO_CREATE: {
    MIN_SCORE: 85,
    MAX_RESULTS: 100,
  },
} as const;

// ==================== Mock Service Configuration ====================
export const MOCK = {
  NETWORK_DELAY: {
    MIN_MS: 200,
    MAX_MS: 700,
  },
  ENTITY_ID_OFFSET: 10000, // Mock IDs start at 10000
  RANDOM_ID_RANGE: 100000,
} as const;

// ==================== Rate Limiting ====================
export const RATE_LIMIT = {
  WINDOW_MS: 15 * 60 * 1000, // 15 minutes
  MAX_REQUESTS: 100,
} as const;

// ==================== Logging ====================
export const LOGGING = {
  DEFAULT_LEVEL: 'info',
  LEVELS: ['error', 'warn', 'info', 'debug'] as const,
} as const;

// ==================== Skill Matching ====================
export const SKILL_SYNONYMS: Record<string, string[]> = {
  javascript: ['js', 'ecmascript'],
  typescript: ['ts'],
  python: ['py'],
  react: ['reactjs', 'react.js'],
  node: ['nodejs', 'node.js'],
  sql: ['structured query language'],
  nosql: ['mongodb', 'cassandra', 'dynamodb'],
} as const;

// ==================== Bullhorn Configuration ====================
export const BULLHORN = {
  AUTH: {
    TOKEN_BUFFER_MS: 60000, // Refresh 1 minute before expiry
  },
  REQUEST: {
    TIMEOUT_MS: 30000,
    DEFAULT_COUNT: 500,
  },
  SUBMISSION: {
    DEFAULT_STATUS: 'New Lead',
    DEFAULT_SOURCE: 'Referral System',
  },
} as const;

// ==================== Time Constants ====================
export const TIME = {
  SECOND_MS: 1000,
  MINUTE_MS: 60 * 1000,
  HOUR_MS: 60 * 60 * 1000,
  DAY_MS: 24 * 60 * 60 * 1000,
} as const;

// ==================== Field Lengths ====================
export const FIELD_LENGTH = {
  NAME: 50,
  EMAIL: 255,
  PHONE: 20,
  TEXT_SHORT: 100,
  TEXT_MEDIUM: 255,
  TEXT_LONG: 1000,
} as const;
