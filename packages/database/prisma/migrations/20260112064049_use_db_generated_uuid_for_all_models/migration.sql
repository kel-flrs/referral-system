-- AlterTable
ALTER TABLE "Candidate" ALTER COLUMN "id" SET DEFAULT gen_random_uuid();

-- AlterTable
ALTER TABLE "CandidateConnection" ALTER COLUMN "id" SET DEFAULT gen_random_uuid();

-- AlterTable
ALTER TABLE "Consultant" ALTER COLUMN "id" SET DEFAULT gen_random_uuid();

-- AlterTable
ALTER TABLE "ConsultantActivity" ALTER COLUMN "id" SET DEFAULT gen_random_uuid();

-- AlterTable
ALTER TABLE "Job" ALTER COLUMN "id" SET DEFAULT gen_random_uuid();

-- AlterTable
ALTER TABLE "Position" ALTER COLUMN "id" SET DEFAULT gen_random_uuid();

-- AlterTable
ALTER TABLE "Referral" ALTER COLUMN "id" SET DEFAULT gen_random_uuid();

-- AlterTable
ALTER TABLE "pipeline_runs" ALTER COLUMN "id" SET DEFAULT gen_random_uuid();
