CREATE TABLE assessment.attempt_submit_requests (
 id UUID PRIMARY KEY, attempt_id UUID NOT NULL REFERENCES assessment.attempts(id), candidate_user_id UUID NOT NULL,
 idempotency_key VARCHAR(200) NOT NULL, submitted_at TIMESTAMPTZ NOT NULL,
 CONSTRAINT attempt_submit_requests_key_unique UNIQUE (attempt_id, idempotency_key)
);
CREATE INDEX attempt_submit_requests_attempt_id_idx ON assessment.attempt_submit_requests (attempt_id);
