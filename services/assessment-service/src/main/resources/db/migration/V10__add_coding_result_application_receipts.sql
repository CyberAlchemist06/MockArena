CREATE TABLE assessment.coding_result_application_receipts (
 evaluation_job_id UUID PRIMARY KEY,
 attempt_id UUID NOT NULL,
 global_position INTEGER NOT NULL,
 question_version_id UUID NOT NULL,
 source_fingerprint VARCHAR(64),
 created_at TIMESTAMPTZ NOT NULL,
 FOREIGN KEY (attempt_id, global_position) REFERENCES assessment.attempt_items(attempt_id, global_position)
);
