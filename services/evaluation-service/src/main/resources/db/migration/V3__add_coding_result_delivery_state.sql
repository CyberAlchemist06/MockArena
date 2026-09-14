ALTER TABLE evaluation.coding_evaluation_jobs DROP CONSTRAINT coding_evaluation_jobs_status_check;
ALTER TABLE evaluation.coding_evaluation_jobs ADD CONSTRAINT coding_evaluation_jobs_status_check CHECK (status IN ('RECEIVED','RESOLVING_SPEC','READY_FOR_RUNNER','RUNNING','READY_FOR_RESULT_APPLICATION','RETRY_WAIT','COMPLETED','FAILED_INFRASTRUCTURE'));
