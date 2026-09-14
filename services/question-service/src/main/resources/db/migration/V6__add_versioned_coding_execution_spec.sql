-- Optional by design: existing immutable coding QuestionVersions remain
-- candidate-deliverable legacy content, but are not executable by a runner.
ALTER TABLE question.question_versions
    ADD COLUMN coding_execution_spec JSONB;

ALTER TABLE question.question_versions
    ADD CONSTRAINT question_versions_coding_execution_spec_object_check
    CHECK (coding_execution_spec IS NULL OR jsonb_typeof(coding_execution_spec) = 'object');
