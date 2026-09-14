CREATE TABLE assessment.submitted_coding_response_snapshots (
    attempt_id UUID NOT NULL,
    global_position INTEGER NOT NULL,
    question_id UUID NOT NULL,
    question_version_id UUID NOT NULL,
    source_fingerprint VARCHAR(64),
    response_state VARCHAR(16) NOT NULL,
    programming_language VARCHAR(64),
    source_code TEXT,
    response_version BIGINT,
    submitted_at TIMESTAMPTZ NOT NULL,
    PRIMARY KEY (attempt_id, global_position),
    FOREIGN KEY (attempt_id, global_position) REFERENCES assessment.attempt_items(attempt_id, global_position),
    CONSTRAINT submitted_coding_response_snapshot_state_check CHECK (response_state IN ('ANSWERED', 'UNANSWERED')),
    CONSTRAINT submitted_coding_response_snapshot_payload_check CHECK (
        (response_state = 'ANSWERED' AND programming_language IS NOT NULL AND source_code IS NOT NULL AND response_version IS NOT NULL AND source_fingerprint IS NOT NULL)
        OR
        (response_state = 'UNANSWERED' AND programming_language IS NULL AND source_code IS NULL AND response_version IS NULL AND source_fingerprint IS NULL)
    )
);

CREATE TABLE assessment.coding_evaluation_outbox (
    outbox_id UUID PRIMARY KEY,
    attempt_id UUID NOT NULL,
    global_position INTEGER NOT NULL,
    question_version_id UUID NOT NULL,
    source_fingerprint VARCHAR(64),
    event_type VARCHAR(64) NOT NULL,
    delivery_status VARCHAR(16) NOT NULL,
    delivery_attempts INTEGER NOT NULL DEFAULT 0,
    next_attempt_at TIMESTAMPTZ NOT NULL,
    lease_owner VARCHAR(128),
    lease_until TIMESTAMPTZ,
    delivered_at TIMESTAMPTZ,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    UNIQUE (attempt_id, global_position, event_type),
    FOREIGN KEY (attempt_id, global_position) REFERENCES assessment.submitted_coding_response_snapshots(attempt_id, global_position),
    CONSTRAINT coding_evaluation_outbox_type_check CHECK (event_type = 'CODING_EVALUATION_REQUESTED'),
    CONSTRAINT coding_evaluation_outbox_status_check CHECK (delivery_status IN ('PENDING', 'IN_FLIGHT', 'DELIVERED')),
    CONSTRAINT coding_evaluation_outbox_attempts_check CHECK (delivery_attempts >= 0)
);

CREATE INDEX coding_evaluation_outbox_delivery_idx
    ON assessment.coding_evaluation_outbox(delivery_status, next_attempt_at, created_at);

CREATE OR REPLACE FUNCTION assessment.prevent_terminal_attempt_response_mutation()
RETURNS TRIGGER AS $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM assessment.attempts
        WHERE id = COALESCE(NEW.attempt_id, OLD.attempt_id)
          AND status <> 'IN_PROGRESS'
    ) THEN
        RAISE EXCEPTION 'attempt responses are immutable after attempt submission';
    END IF;
    RETURN COALESCE(NEW, OLD);
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER attempt_item_responses_reject_terminal_mutation
BEFORE INSERT OR UPDATE OR DELETE ON assessment.attempt_item_responses
FOR EACH ROW EXECUTE FUNCTION assessment.prevent_terminal_attempt_response_mutation();

CREATE OR REPLACE FUNCTION assessment.prevent_submitted_coding_snapshot_change()
RETURNS TRIGGER AS $$
BEGIN
    IF TG_OP IN ('UPDATE', 'DELETE') THEN
        RAISE EXCEPTION 'submitted coding response snapshots are immutable';
    END IF;
    IF NOT EXISTS (
        SELECT 1 FROM assessment.attempts
        WHERE id = NEW.attempt_id AND status = 'SUBMITTED'
    ) THEN
        RAISE EXCEPTION 'submitted coding response snapshots require submitted attempt';
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER submitted_coding_response_snapshots_immutable
BEFORE INSERT OR UPDATE OR DELETE ON assessment.submitted_coding_response_snapshots
FOR EACH ROW EXECUTE FUNCTION assessment.prevent_submitted_coding_snapshot_change();
