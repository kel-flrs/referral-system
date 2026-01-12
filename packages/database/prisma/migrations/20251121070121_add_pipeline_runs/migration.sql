-- CreateTable
CREATE TABLE "pipeline_runs" (
    "id" TEXT NOT NULL,
    "pipeline_name" TEXT NOT NULL,
    "status" TEXT NOT NULL,
    "records_processed" INTEGER NOT NULL DEFAULT 0,
    "records_inserted" INTEGER NOT NULL DEFAULT 0,
    "records_updated" INTEGER NOT NULL DEFAULT 0,
    "error_message" TEXT,
    "metadata" JSONB,
    "duration_seconds" DOUBLE PRECISION,
    "started_at" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "completed_at" TIMESTAMP(3),

    CONSTRAINT "pipeline_runs_pkey" PRIMARY KEY ("id")
);

-- CreateIndex
CREATE INDEX "pipeline_runs_pipeline_name_idx" ON "pipeline_runs"("pipeline_name");

-- CreateIndex
CREATE INDEX "pipeline_runs_status_idx" ON "pipeline_runs"("status");

-- CreateIndex
CREATE INDEX "pipeline_runs_started_at_idx" ON "pipeline_runs"("started_at");
