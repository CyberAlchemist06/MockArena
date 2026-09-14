CREATE TABLE assessment.attempt_item_responses (
    attempt_id UUID NOT NULL,
    global_position INTEGER NOT NULL,
    question_type_code VARCHAR(64) NOT NULL,
    response_payload JSONB NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    PRIMARY KEY (attempt_id, global_position),
    CONSTRAINT attempt_item_responses_route_fk
        FOREIGN KEY (attempt_id, global_position)
        REFERENCES assessment.attempt_items (attempt_id, global_position),
    CONSTRAINT attempt_item_responses_question_type_check
        CHECK (length(trim(question_type_code)) > 0),
    CONSTRAINT attempt_item_responses_payload_object_check
        CHECK (jsonb_typeof(response_payload) = 'object')
);

CREATE TABLE assessment.attempt_response_save_requests (
    id UUID PRIMARY KEY,
    attempt_id UUID NOT NULL,
    idempotency_key VARCHAR(200) NOT NULL,
    client_mutation_id UUID NOT NULL,
    request_fingerprint VARCHAR(128) NOT NULL,
    global_position INTEGER NOT NULL,
    saved_response_version BIGINT NOT NULL,
    state VARCHAR(32) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT attempt_response_save_requests_key_unique UNIQUE (attempt_id, idempotency_key),
    CONSTRAINT attempt_response_save_requests_mutation_unique UNIQUE (attempt_id, client_mutation_id),
    CONSTRAINT attempt_response_save_requests_response_fk
        FOREIGN KEY (attempt_id, global_position)
        REFERENCES assessment.attempt_item_responses (attempt_id, global_position)
);

CREATE INDEX attempt_item_responses_attempt_position_idx
    ON assessment.attempt_item_responses (attempt_id, global_position);
