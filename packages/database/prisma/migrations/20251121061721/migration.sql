-- Enable pgvector extension
CREATE EXTENSION IF NOT EXISTS vector;

-- CreateTable
CREATE TABLE "Consultant" (
    "id" TEXT NOT NULL,
    "bullhornId" TEXT NOT NULL,
    "firstName" TEXT NOT NULL,
    "lastName" TEXT NOT NULL,
    "email" TEXT NOT NULL,
    "phone" TEXT,
    "isActive" BOOLEAN NOT NULL DEFAULT true,
    "lastActivityAt" TIMESTAMP(3),
    "totalPlacements" INTEGER NOT NULL DEFAULT 0,
    "totalReferrals" INTEGER NOT NULL DEFAULT 0,
    "preferenceEmbedding" vector(384),
    "createdAt" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updatedAt" TIMESTAMP(3) NOT NULL,

    CONSTRAINT "Consultant_pkey" PRIMARY KEY ("id")
);

-- CreateTable
CREATE TABLE "ConsultantActivity" (
    "id" TEXT NOT NULL,
    "consultantId" TEXT NOT NULL,
    "bullhornId" TEXT NOT NULL,
    "activityType" TEXT NOT NULL,
    "subject" TEXT,
    "description" TEXT,
    "contactBullhornId" TEXT,
    "contactName" TEXT,
    "contactEmail" TEXT,
    "activityDate" TIMESTAMP(3) NOT NULL,
    "createdAt" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updatedAt" TIMESTAMP(3) NOT NULL,

    CONSTRAINT "ConsultantActivity_pkey" PRIMARY KEY ("id")
);

-- CreateTable
CREATE TABLE "Candidate" (
    "id" TEXT NOT NULL,
    "bullhornId" TEXT NOT NULL,
    "firstName" TEXT NOT NULL,
    "lastName" TEXT NOT NULL,
    "email" TEXT NOT NULL,
    "phone" TEXT,
    "currentTitle" TEXT,
    "currentCompany" TEXT,
    "location" TEXT,
    "skills" TEXT[],
    "summary" TEXT,
    "experience" JSONB,
    "education" JSONB,
    "status" TEXT NOT NULL DEFAULT 'ACTIVE',
    "profileEmbedding" vector(384),
    "lastSyncedAt" TIMESTAMP(3),
    "createdAt" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updatedAt" TIMESTAMP(3) NOT NULL,

    CONSTRAINT "Candidate_pkey" PRIMARY KEY ("id")
);

-- CreateTable
CREATE TABLE "CandidateConnection" (
    "id" TEXT NOT NULL,
    "candidateId" TEXT NOT NULL,
    "connectionType" TEXT NOT NULL,
    "connectedName" TEXT NOT NULL,
    "connectedEmail" TEXT,
    "connectedTitle" TEXT,
    "connectedCompany" TEXT,
    "connectedBullhornId" TEXT,
    "relationshipStrength" INTEGER NOT NULL DEFAULT 1,
    "notes" TEXT,
    "source" TEXT,
    "verifiedAt" TIMESTAMP(3),
    "createdAt" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updatedAt" TIMESTAMP(3) NOT NULL,

    CONSTRAINT "CandidateConnection_pkey" PRIMARY KEY ("id")
);

-- CreateTable
CREATE TABLE "Position" (
    "id" TEXT NOT NULL,
    "bullhornId" TEXT NOT NULL,
    "title" TEXT NOT NULL,
    "description" TEXT,
    "employmentType" TEXT,
    "requiredSkills" TEXT[],
    "preferredSkills" TEXT[],
    "experienceLevel" TEXT,
    "location" TEXT,
    "salary" TEXT,
    "clientName" TEXT,
    "clientBullhornId" TEXT,
    "status" TEXT NOT NULL DEFAULT 'OPEN',
    "openDate" TIMESTAMP(3),
    "closeDate" TIMESTAMP(3),
    "descriptionEmbedding" vector(384),
    "lastSyncedAt" TIMESTAMP(3),
    "createdAt" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updatedAt" TIMESTAMP(3) NOT NULL,

    CONSTRAINT "Position_pkey" PRIMARY KEY ("id")
);

-- CreateTable
CREATE TABLE "Match" (
    "id" TEXT NOT NULL,
    "candidateId" TEXT NOT NULL,
    "positionId" TEXT NOT NULL,
    "overallScore" DOUBLE PRECISION NOT NULL,
    "skillMatchScore" DOUBLE PRECISION NOT NULL,
    "experienceScore" DOUBLE PRECISION NOT NULL,
    "locationScore" DOUBLE PRECISION,
    "semanticScore" DOUBLE PRECISION,
    "matchedSkills" TEXT[],
    "missingSkills" TEXT[],
    "matchReason" TEXT,
    "status" TEXT NOT NULL DEFAULT 'PENDING',
    "createdAt" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updatedAt" TIMESTAMP(3) NOT NULL,

    CONSTRAINT "Match_pkey" PRIMARY KEY ("id")
);

-- CreateTable
CREATE TABLE "Referral" (
    "id" TEXT NOT NULL,
    "matchId" TEXT NOT NULL,
    "candidateId" TEXT NOT NULL,
    "positionId" TEXT NOT NULL,
    "consultantId" TEXT NOT NULL,
    "referralSource" TEXT NOT NULL,
    "referrerName" TEXT,
    "referrerEmail" TEXT,
    "referrerPhone" TEXT,
    "notes" TEXT,
    "bullhornSubmissionId" TEXT,
    "sentToBullhornAt" TIMESTAMP(3),
    "status" TEXT NOT NULL DEFAULT 'PENDING',
    "createdAt" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updatedAt" TIMESTAMP(3) NOT NULL,

    CONSTRAINT "Referral_pkey" PRIMARY KEY ("id")
);

-- CreateTable
CREATE TABLE "Job" (
    "id" TEXT NOT NULL,
    "type" TEXT NOT NULL,
    "status" TEXT NOT NULL DEFAULT 'PENDING',
    "data" JSONB,
    "result" JSONB,
    "error" TEXT,
    "attempts" INTEGER NOT NULL DEFAULT 0,
    "maxAttempts" INTEGER NOT NULL DEFAULT 3,
    "startedAt" TIMESTAMP(3),
    "completedAt" TIMESTAMP(3),
    "createdAt" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updatedAt" TIMESTAMP(3) NOT NULL,

    CONSTRAINT "Job_pkey" PRIMARY KEY ("id")
);

-- CreateIndex
CREATE UNIQUE INDEX "Consultant_bullhornId_key" ON "Consultant"("bullhornId");

-- CreateIndex
CREATE UNIQUE INDEX "Consultant_email_key" ON "Consultant"("email");

-- CreateIndex
CREATE INDEX "Consultant_bullhornId_idx" ON "Consultant"("bullhornId");

-- CreateIndex
CREATE INDEX "Consultant_email_idx" ON "Consultant"("email");

-- CreateIndex
CREATE UNIQUE INDEX "ConsultantActivity_bullhornId_key" ON "ConsultantActivity"("bullhornId");

-- CreateIndex
CREATE INDEX "ConsultantActivity_consultantId_idx" ON "ConsultantActivity"("consultantId");

-- CreateIndex
CREATE INDEX "ConsultantActivity_contactBullhornId_idx" ON "ConsultantActivity"("contactBullhornId");

-- CreateIndex
CREATE INDEX "ConsultantActivity_activityDate_idx" ON "ConsultantActivity"("activityDate");

-- CreateIndex
CREATE UNIQUE INDEX "Candidate_bullhornId_key" ON "Candidate"("bullhornId");

-- CreateIndex
CREATE INDEX "Candidate_bullhornId_idx" ON "Candidate"("bullhornId");

-- CreateIndex
CREATE INDEX "Candidate_email_idx" ON "Candidate"("email");

-- CreateIndex
CREATE INDEX "Candidate_status_idx" ON "Candidate"("status");

-- CreateIndex
CREATE INDEX "CandidateConnection_candidateId_idx" ON "CandidateConnection"("candidateId");

-- CreateIndex
CREATE INDEX "CandidateConnection_connectedEmail_idx" ON "CandidateConnection"("connectedEmail");

-- CreateIndex
CREATE INDEX "CandidateConnection_connectedBullhornId_idx" ON "CandidateConnection"("connectedBullhornId");

-- CreateIndex
CREATE UNIQUE INDEX "Position_bullhornId_key" ON "Position"("bullhornId");

-- CreateIndex
CREATE INDEX "Position_bullhornId_idx" ON "Position"("bullhornId");

-- CreateIndex
CREATE INDEX "Position_status_idx" ON "Position"("status");

-- CreateIndex
CREATE INDEX "Match_overallScore_idx" ON "Match"("overallScore");

-- CreateIndex
CREATE INDEX "Match_status_idx" ON "Match"("status");

-- CreateIndex
CREATE INDEX "Match_createdAt_idx" ON "Match"("createdAt");

-- CreateIndex
CREATE INDEX "Match_status_overallScore_createdAt_idx" ON "Match"("status", "overallScore", "createdAt");

-- CreateIndex
CREATE UNIQUE INDEX "Match_candidateId_positionId_key" ON "Match"("candidateId", "positionId");

-- CreateIndex
CREATE UNIQUE INDEX "Referral_matchId_key" ON "Referral"("matchId");

-- CreateIndex
CREATE UNIQUE INDEX "Referral_bullhornSubmissionId_key" ON "Referral"("bullhornSubmissionId");

-- CreateIndex
CREATE INDEX "Referral_candidateId_idx" ON "Referral"("candidateId");

-- CreateIndex
CREATE INDEX "Referral_positionId_idx" ON "Referral"("positionId");

-- CreateIndex
CREATE INDEX "Referral_consultantId_idx" ON "Referral"("consultantId");

-- CreateIndex
CREATE INDEX "Referral_status_idx" ON "Referral"("status");

-- CreateIndex
CREATE INDEX "Referral_createdAt_idx" ON "Referral"("createdAt");

-- CreateIndex
CREATE INDEX "Referral_sentToBullhornAt_idx" ON "Referral"("sentToBullhornAt");

-- CreateIndex
CREATE INDEX "Referral_status_createdAt_idx" ON "Referral"("status", "createdAt");

-- CreateIndex
CREATE INDEX "Job_type_idx" ON "Job"("type");

-- CreateIndex
CREATE INDEX "Job_status_idx" ON "Job"("status");

-- AddForeignKey
ALTER TABLE "ConsultantActivity" ADD CONSTRAINT "ConsultantActivity_consultantId_fkey" FOREIGN KEY ("consultantId") REFERENCES "Consultant"("id") ON DELETE CASCADE ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE "CandidateConnection" ADD CONSTRAINT "CandidateConnection_candidateId_fkey" FOREIGN KEY ("candidateId") REFERENCES "Candidate"("id") ON DELETE CASCADE ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE "Match" ADD CONSTRAINT "Match_candidateId_fkey" FOREIGN KEY ("candidateId") REFERENCES "Candidate"("id") ON DELETE CASCADE ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE "Match" ADD CONSTRAINT "Match_positionId_fkey" FOREIGN KEY ("positionId") REFERENCES "Position"("id") ON DELETE CASCADE ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE "Referral" ADD CONSTRAINT "Referral_matchId_fkey" FOREIGN KEY ("matchId") REFERENCES "Match"("id") ON DELETE CASCADE ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE "Referral" ADD CONSTRAINT "Referral_candidateId_fkey" FOREIGN KEY ("candidateId") REFERENCES "Candidate"("id") ON DELETE CASCADE ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE "Referral" ADD CONSTRAINT "Referral_positionId_fkey" FOREIGN KEY ("positionId") REFERENCES "Position"("id") ON DELETE CASCADE ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE "Referral" ADD CONSTRAINT "Referral_consultantId_fkey" FOREIGN KEY ("consultantId") REFERENCES "Consultant"("id") ON DELETE CASCADE ON UPDATE CASCADE;
