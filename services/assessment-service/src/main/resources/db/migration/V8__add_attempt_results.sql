CREATE TABLE assessment.attempt_results (
 attempt_id UUID PRIMARY KEY REFERENCES assessment.attempts(id),
 evaluation_status VARCHAR(32) NOT NULL,
 raw_score NUMERIC(12,2), max_score NUMERIC(12,2), percentage NUMERIC(7,4),
 evaluated_at TIMESTAMPTZ, released_at TIMESTAMPTZ,
 version BIGINT NOT NULL DEFAULT 0, created_at TIMESTAMPTZ NOT NULL, updated_at TIMESTAMPTZ NOT NULL
);
CREATE TABLE assessment.attempt_item_results (
 attempt_id UUID NOT NULL, global_position INTEGER NOT NULL, question_id UUID NOT NULL, question_version_id UUID NOT NULL,
 question_type_code VARCHAR(64) NOT NULL, evaluation_status VARCHAR(32) NOT NULL, outcome VARCHAR(32),
 awarded_score NUMERIC(12,2), max_score NUMERIC(12,2), evaluated_at TIMESTAMPTZ,
 PRIMARY KEY (attempt_id, global_position),
 FOREIGN KEY (attempt_id, global_position) REFERENCES assessment.attempt_items(attempt_id, global_position)
);
CREATE INDEX attempt_item_results_attempt_idx ON assessment.attempt_item_results(attempt_id);
