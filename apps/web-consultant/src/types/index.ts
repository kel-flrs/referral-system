export type MatchStatus = 'PENDING' | 'REVIEWED' | 'REFERRED' | 'REJECTED';
export type ReferralStatus = 'PENDING' | 'SENT' | 'CONTACTED' | 'INTERVIEWING' | 'PLACED' | 'REJECTED';

export interface Consultant {
  id: string;
  firstName: string;
  lastName: string;
  email: string;
  bullhornId?: string;
  isActive?: boolean;
  createdAt?: string;
  updatedAt?: string;
}

export interface Candidate {
  id: string;
  firstName: string;
  lastName: string;
  email?: string;
  phone?: string | null;
  skills?: string[];
  currentTitle?: string | null;
  currentCompany?: string | null;
  location?: string | null;
  summary?: string | null;
  experience?: unknown;
  education?: unknown;
  status?: string;
  bullhornId?: string;
  createdAt?: string;
  updatedAt?: string;
}

export interface Position {
  id: string;
  title: string;
  clientName?: string | null;
  description?: string | null;
  requiredSkills?: string[];
  preferredSkills?: string[];
  experienceLevel?: string | null;
  location?: string | null;
  salary?: string | null;
  status?: string;
  bullhornId?: string;
  createdAt?: string;
  updatedAt?: string;
}

export interface Match {
  id: string;
  candidateId: string;
  positionId: string;
  overallScore: number;
  skillMatchScore: number;
  experienceScore: number;
  locationScore?: number | null;
  semanticScore?: number | null;
  status: MatchStatus;
  matchedSkills: string[];
  missingSkills: string[];
  matchReason?: string | null;
  createdAt: string;
  updatedAt: string;
  candidate: Candidate;
  position: Position;
}

export interface Referral {
  id: string;
  candidateId: string;
  positionId: string;
  consultantId: string;
  matchId: string;
  status: ReferralStatus;
  referralSource: string;
  notes?: string | null;
  sentToBullhornAt?: string | null;
  createdAt: string;
  updatedAt: string;
  candidate: Candidate;
  position: Position;
  consultant: Consultant;
  match?: {
    overallScore: number;
    matchReason?: string | null;
  };
}

export interface DashboardStats {
  overview: {
    consultants: { total: number; active: number };
    candidates: { total: number; active: number };
    positions: { total: number; open: number };
    matches: { total: number; pending: number };
    referrals: { total: number; pending: number; sent: number };
  };
  topConsultants: {
    id: string;
    firstName: string;
    lastName: string;
    totalReferrals: number;
    totalPlacements: number;
  }[];
  recentMatches: Match[];
}

export interface ApiResponse<T> {
  data: T;
  success: boolean;
  message?: string;
}

export interface PaginatedResponse<T> {
  data: T[];
  total: number;
  page: number;
  limit: number;
  totalPages: number;
}
