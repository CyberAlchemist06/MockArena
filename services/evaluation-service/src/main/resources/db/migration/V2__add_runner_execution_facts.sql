ALTER TABLE evaluation.coding_evaluation_jobs
  ADD COLUMN candidate_failure_category VARCHAR(64),
  ADD COLUMN tests_passed INTEGER,
  ADD COLUMN tests_total INTEGER,
  ADD COLUMN execution_time_ms BIGINT;
ALTER TABLE evaluation.coding_evaluation_jobs
  DROP CONSTRAINT coding_evaluation_jobs_status_check,
  ADD CONSTRAINT coding_evaluation_jobs_status_check CHECK (status IN ('RECEIVED','RESOLVING_SPEC','READY_FOR_RUNNER','RUNNING','READY_FOR_RESULT_APPLICATION','RETRY_WAIT','FAILED_INFRASTRUCTURE'));
