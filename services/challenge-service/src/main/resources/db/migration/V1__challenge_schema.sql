CREATE SCHEMA IF NOT EXISTS challenge;

CREATE TABLE challenge.challenges (
    id UUID PRIMARY KEY,
    visibility VARCHAR(24) NOT NULL,
    lifecycle_status VARCHAR(16) NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT challenges_visibility_check CHECK (visibility IN ('PRIVATE', 'SHARED', 'PUBLIC', 'ORGANIZATION_ONLY')),
    CONSTRAINT challenges_lifecycle_check CHECK (lifecycle_status IN ('DRAFT', 'PUBLISHED', 'ARCHIVED'))
);

CREATE TABLE challenge.challenge_versions (
    id UUID PRIMARY KEY,
    challenge_id UUID NOT NULL REFERENCES challenge.challenges(id),
    version_number INTEGER NOT NULL,
    status VARCHAR(16) NOT NULL,
    title VARCHAR(200) NOT NULL,
    tags_all JSONB NOT NULL DEFAULT '[]'::jsonb,
    difficulties JSONB NOT NULL DEFAULT '[]'::jsonb,
    supported_language VARCHAR(32),
    requested_question_count INTEGER NOT NULL,
    selection_seed UUID NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT challenge_versions_status_check CHECK (status IN ('DRAFT', 'PUBLISHED', 'RETIRED')),
    CONSTRAINT challenge_versions_count_check CHECK (requested_question_count > 0),
    CONSTRAINT challenge_versions_number_unique UNIQUE (challenge_id, version_number)
);

CREATE TABLE challenge.challenge_version_questions (
    challenge_version_id UUID NOT NULL REFERENCES challenge.challenge_versions(id),
    position INTEGER NOT NULL,
    question_id UUID NOT NULL,
    question_version_id UUID NOT NULL,
    PRIMARY KEY (challenge_version_id, position),
    UNIQUE (challenge_version_id, question_version_id),
    UNIQUE (challenge_version_id, question_id)
);

CREATE INDEX challenge_versions_challenge_id_idx ON challenge.challenge_versions (challenge_id, version_number);
CREATE INDEX challenge_version_questions_question_version_id_idx ON challenge.challenge_version_questions (question_version_id);

CREATE OR REPLACE FUNCTION challenge.prevent_published_challenge_version_change()
RETURNS TRIGGER AS $$
BEGIN
    IF TG_OP IN ('UPDATE', 'DELETE') AND OLD.status = 'PUBLISHED' THEN
        RAISE EXCEPTION 'published challenge versions are immutable';
    END IF;
    RETURN COALESCE(NEW, OLD);
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER challenge_versions_immutable_after_publication
BEFORE UPDATE OR DELETE ON challenge.challenge_versions
FOR EACH ROW EXECUTE FUNCTION challenge.prevent_published_challenge_version_change();

CREATE OR REPLACE FUNCTION challenge.prevent_published_composition_change()
RETURNS TRIGGER AS $$
BEGIN
    IF EXISTS (SELECT 1 FROM challenge.challenge_versions WHERE id = COALESCE(NEW.challenge_version_id, OLD.challenge_version_id) AND status = 'PUBLISHED') THEN
        RAISE EXCEPTION 'published challenge version composition is immutable';
    END IF;
    RETURN COALESCE(NEW, OLD);
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER challenge_version_questions_immutable_after_publication
BEFORE INSERT OR UPDATE OR DELETE ON challenge.challenge_version_questions
FOR EACH ROW EXECUTE FUNCTION challenge.prevent_published_composition_change();
