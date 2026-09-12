ALTER TABLE question.question_versions
    ADD COLUMN question_type VARCHAR(16) NOT NULL DEFAULT 'CODING',
    ADD COLUMN options JSONB NOT NULL DEFAULT '[]'::jsonb,
    ADD COLUMN correct_option_id VARCHAR(64),
    ADD COLUMN explanation TEXT;

ALTER TABLE question.question_versions
    ALTER COLUMN examples DROP NOT NULL,
    ALTER COLUMN supported_languages DROP NOT NULL,
    ALTER COLUMN visible_tests DROP NOT NULL,
    ALTER COLUMN hidden_tests DROP NOT NULL,
    ALTER COLUMN scoring_rules DROP NOT NULL,
    ALTER COLUMN execution_limits DROP NOT NULL;

ALTER TABLE question.question_versions
    ADD CONSTRAINT question_versions_question_type_check
    CHECK (question_type IN ('MCQ', 'CODING'));
