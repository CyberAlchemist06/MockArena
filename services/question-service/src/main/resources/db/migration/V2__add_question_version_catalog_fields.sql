ALTER TABLE question.question_versions
    ADD COLUMN tags JSONB NOT NULL DEFAULT '[]'::jsonb,
    ADD COLUMN difficulty VARCHAR(16) NOT NULL DEFAULT 'MEDIUM';

ALTER TABLE question.question_versions
    ADD CONSTRAINT question_versions_difficulty_check
    CHECK (difficulty IN ('EASY', 'MEDIUM', 'HARD'));

CREATE INDEX question_versions_published_difficulty_idx
    ON question.question_versions (difficulty, question_id, id)
    WHERE status = 'PUBLISHED';

CREATE INDEX question_versions_tags_gin_idx
    ON question.question_versions USING GIN (tags jsonb_path_ops);

CREATE INDEX question_versions_supported_languages_gin_idx
    ON question.question_versions USING GIN (supported_languages jsonb_path_ops);
