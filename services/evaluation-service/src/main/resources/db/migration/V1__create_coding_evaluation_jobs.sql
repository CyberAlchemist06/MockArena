CREATE TABLE evaluation.coding_evaluation_jobs (
    job_id UUID PRIMARY KEY,
    attempt_id UUID NOT NULL,
    global_position INTEGER NOT NULL CHECK (global_position > 0),
    question_id UUID NOT NULL,
    question_version_id UUID NOT NULL,
    submitted_response_fingerprint VARCHAR(64),
    programming_language VARCHAR(64),
    status VARCHAR(32) NOT NULL,
    retry_count INTEGER NOT NULL DEFAULT 0 CHECK (retry_count >= 0),
    next_attempt_at TIMESTAMPTZ NOT NULL,
    lease_owner VARCHAR(128),
    lease_expires_at TIMESTAMPTZ,
    last_failure_category VARCHAR(64),
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT coding_evaluation_jobs_status_check CHECK (status IN ('RECEIVED','RESOLVING_SPEC','READY_FOR_RUNNER','READY_FOR_RESULT_APPLICATION','RETRY_WAIT','FAILED_INFRASTRUCTURE')),
    CONSTRAINT coding_evaluation_jobs_identity_check CHECK ((submitted_response_fingerprint IS NOT NULL AND programming_language IS NOT NULL) OR (submitted_response_fingerprint IS NULL AND programming_language IS NULL)),
    CONSTRAINT coding_evaluation_jobs_business_identity_check CHECK (submitted_response_fingerprint IS NOT NULL OR programming_language IS NULL)
);
CREATE UNIQUE INDEX coding_evaluation_jobs_business_key_uq ON evaluation.coding_evaluation_jobs (attempt_id, global_position, question_version_id, COALESCE(submitted_response_fingerprint, 'UNANSWERED'));
CREATE INDEX coding_evaluation_jobs_claim_idx ON evaluation.coding_evaluation_jobs(status, next_attempt_at, lease_expires_at, created_at);
