CREATE SCHEMA IF NOT EXISTS assessment;

CREATE TABLE assessment.assessments (
    id UUID PRIMARY KEY,
    created_by_user_id UUID NOT NULL,
    visibility VARCHAR(24) NOT NULL,
    lifecycle_status VARCHAR(16) NOT NULL,
    current_published_version_id UUID,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT assessments_visibility_check CHECK (visibility IN ('PRIVATE', 'SHARED', 'PUBLIC', 'ORGANIZATION_ONLY')),
    CONSTRAINT assessments_lifecycle_check CHECK (lifecycle_status IN ('DRAFT', 'PUBLISHED', 'CLOSED', 'ARCHIVED'))
);

CREATE TABLE assessment.assessment_versions (
    id UUID PRIMARY KEY,
    assessment_id UUID NOT NULL REFERENCES assessment.assessments(id),
    version_number INTEGER NOT NULL,
    status VARCHAR(16) NOT NULL,
    title VARCHAR(200) NOT NULL,
    description TEXT,
    instructions TEXT,
    assessment_type_code VARCHAR(64) NOT NULL,
    timing_policy_code VARCHAR(64) NOT NULL,
    timing_policy_parameters JSONB NOT NULL,
    attempt_policy_code VARCHAR(64) NOT NULL,
    attempt_policy_parameters JSONB NOT NULL,
    result_release_policy_code VARCHAR(64) NOT NULL,
    result_release_policy_parameters JSONB NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT assessment_versions_status_check CHECK (status IN ('DRAFT', 'PUBLISHED', 'RETIRED')),
    CONSTRAINT assessment_versions_number_unique UNIQUE (assessment_id, version_number)
);

CREATE TABLE assessment.assessment_version_challenges (
    assessment_version_id UUID NOT NULL REFERENCES assessment.assessment_versions(id),
    position INTEGER NOT NULL,
    challenge_id UUID NOT NULL,
    challenge_version_id UUID NOT NULL,
    challenge_version_number INTEGER NOT NULL,
    PRIMARY KEY (assessment_version_id, position),
    UNIQUE (assessment_version_id, challenge_version_id),
    CONSTRAINT assessment_version_challenges_position_check CHECK (position > 0)
);

CREATE INDEX assessment_versions_assessment_id_idx ON assessment.assessment_versions (assessment_id, version_number);
CREATE INDEX assessment_version_challenges_version_id_idx ON assessment.assessment_version_challenges (challenge_version_id);

CREATE OR REPLACE FUNCTION assessment.prevent_published_assessment_version_change()
RETURNS TRIGGER AS $$
BEGIN
    IF TG_OP = 'DELETE' AND OLD.status IN ('PUBLISHED', 'RETIRED') THEN
        RAISE EXCEPTION 'published assessment versions are immutable';
    END IF;
    IF TG_OP = 'UPDATE' AND OLD.status = 'PUBLISHED' AND
       (NEW.status <> 'RETIRED' OR
        (to_jsonb(NEW) - ARRAY['status', 'updated_at', 'version']) IS DISTINCT FROM
        (to_jsonb(OLD) - ARRAY['status', 'updated_at', 'version'])) THEN
        RAISE EXCEPTION 'published assessment versions are immutable';
    END IF;
    IF TG_OP = 'UPDATE' AND OLD.status = 'RETIRED' THEN
        RAISE EXCEPTION 'published assessment versions are immutable';
    END IF;
    RETURN COALESCE(NEW, OLD);
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER assessment_versions_immutable_after_publication
BEFORE UPDATE OR DELETE ON assessment.assessment_versions
FOR EACH ROW EXECUTE FUNCTION assessment.prevent_published_assessment_version_change();

CREATE OR REPLACE FUNCTION assessment.prevent_published_assessment_manifest_change()
RETURNS TRIGGER AS $$
BEGIN
    IF EXISTS (SELECT 1 FROM assessment.assessment_versions WHERE id = COALESCE(NEW.assessment_version_id, OLD.assessment_version_id) AND status IN ('PUBLISHED', 'RETIRED')) THEN
        RAISE EXCEPTION 'published assessment version manifest is immutable';
    END IF;
    RETURN COALESCE(NEW, OLD);
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER assessment_version_challenges_immutable_after_publication
BEFORE INSERT OR UPDATE OR DELETE ON assessment.assessment_version_challenges
FOR EACH ROW EXECUTE FUNCTION assessment.prevent_published_assessment_manifest_change();
