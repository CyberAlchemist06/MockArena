CREATE TABLE assessment.attempts (
    id UUID PRIMARY KEY,
    assessment_id UUID NOT NULL,
    assessment_version_id UUID NOT NULL,
    assessment_version_number INTEGER NOT NULL,
    candidate_user_id UUID NOT NULL,
    status VARCHAR(16) NOT NULL,
    started_at TIMESTAMPTZ NOT NULL,
    deadline_at TIMESTAMPTZ,
    submitted_at TIMESTAMPTZ,
    completed_at TIMESTAMPTZ,
    expired_at TIMESTAMPTZ,
    entitlement_reservation_id UUID,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT attempts_status_check CHECK (status IN ('IN_PROGRESS', 'SUBMITTED', 'COMPLETED', 'EXPIRED'))
);

CREATE UNIQUE INDEX attempts_one_active_candidate_version_idx
    ON assessment.attempts (candidate_user_id, assessment_version_id)
    WHERE status = 'IN_PROGRESS';
CREATE INDEX attempts_candidate_version_idx ON assessment.attempts (candidate_user_id, assessment_version_id);

CREATE TABLE assessment.attempt_items (
    attempt_id UUID NOT NULL REFERENCES assessment.attempts(id),
    global_position INTEGER NOT NULL,
    challenge_id UUID NOT NULL,
    challenge_version_id UUID NOT NULL,
    challenge_position INTEGER NOT NULL,
    question_id UUID NOT NULL,
    question_version_id UUID NOT NULL,
    question_position INTEGER NOT NULL,
    question_type_code VARCHAR(64) NOT NULL,
    PRIMARY KEY (attempt_id, global_position),
    CONSTRAINT attempt_items_positions_check CHECK (global_position > 0 AND challenge_position > 0 AND question_position > 0),
    CONSTRAINT attempt_items_question_type_check CHECK (length(trim(question_type_code)) > 0)
);
CREATE INDEX attempt_items_question_version_idx ON assessment.attempt_items (question_version_id);

CREATE TABLE assessment.attempt_start_requests (
    id UUID PRIMARY KEY,
    candidate_user_id UUID NOT NULL,
    idempotency_key VARCHAR(200) NOT NULL,
    request_fingerprint VARCHAR(128) NOT NULL,
    assessment_id UUID NOT NULL,
    assessment_version_id UUID NOT NULL,
    attempt_id UUID NOT NULL REFERENCES assessment.attempts(id),
    state VARCHAR(32) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT attempt_start_requests_key_unique UNIQUE (candidate_user_id, idempotency_key)
);

CREATE TABLE assessment.attempt_entitlement_reconciliations (
    id UUID PRIMARY KEY,
    attempt_id UUID NOT NULL REFERENCES assessment.attempts(id),
    reservation_id UUID NOT NULL,
    capability_code VARCHAR(128) NOT NULL,
    state VARCHAR(32) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT attempt_entitlement_reconciliations_reservation_unique UNIQUE (reservation_id)
);
