ALTER TABLE evaluation.coding_evaluation_jobs ADD COLUMN result_outcome VARCHAR(64);
ALTER TABLE evaluation.coding_evaluation_jobs ADD COLUMN awarded_score NUMERIC(12,2);
ALTER TABLE evaluation.coding_evaluation_jobs ADD COLUMN max_score NUMERIC(12,2);
