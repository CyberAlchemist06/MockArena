CREATE SCHEMA IF NOT EXISTS question;

CREATE TABLE question.questions (
    id UUID PRIMARY KEY,
    owner_user_id UUID NOT NULL,
    lifecycle_status VARCHAR(16) NOT NULL,
    current_version_id UUID,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT questions_lifecycle_status_check CHECK (lifecycle_status IN ('DRAFT', 'PUBLISHED', 'ARCHIVED'))
);

CREATE TABLE question.question_versions (
    id UUID PRIMARY KEY,
    question_id UUID NOT NULL REFERENCES question.questions(id),
    version_number INTEGER NOT NULL,
    status VARCHAR(16) NOT NULL,
    title VARCHAR(200) NOT NULL,
    prompt TEXT NOT NULL,
    constraints_text TEXT,
    examples JSONB NOT NULL DEFAULT '[]'::jsonb,
    supported_languages JSONB NOT NULL DEFAULT '[]'::jsonb,
    visible_tests JSONB NOT NULL DEFAULT '[]'::jsonb,
    hidden_tests JSONB NOT NULL DEFAULT '[]'::jsonb,
    scoring_rules JSONB NOT NULL DEFAULT '{}'::jsonb,
    execution_limits JSONB NOT NULL DEFAULT '{}'::jsonb,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT question_versions_status_check CHECK (status IN ('DRAFT', 'PUBLISHED', 'RETIRED')),
    CONSTRAINT question_versions_question_version_unique UNIQUE (question_id, version_number)
);

ALTER TABLE question.questions ADD CONSTRAINT questions_current_version_fk FOREIGN KEY (current_version_id) REFERENCES question.question_versions(id);

CREATE OR REPLACE FUNCTION question.prevent_published_question_version_change()
RETURNS TRIGGER AS $$
BEGIN
    IF TG_OP = 'DELETE' AND OLD.status = 'PUBLISHED' THEN
        RAISE EXCEPTION 'published question versions are immutable';
    END IF;
    IF TG_OP = 'UPDATE' AND OLD.status = 'PUBLISHED' THEN
        RAISE EXCEPTION 'published question versions are immutable';
    END IF;
    RETURN COALESCE(NEW, OLD);
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER question_versions_immutable_after_publication
BEFORE UPDATE OR DELETE ON question.question_versions
FOR EACH ROW EXECUTE FUNCTION question.prevent_published_question_version_change();
