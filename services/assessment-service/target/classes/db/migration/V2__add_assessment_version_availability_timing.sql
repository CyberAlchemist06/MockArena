ALTER TABLE assessment.assessment_versions
    ADD COLUMN available_from TIMESTAMPTZ,
    ADD COLUMN available_until TIMESTAMPTZ,
    ADD COLUMN attempt_duration_seconds INTEGER;

UPDATE assessment.assessment_versions
SET attempt_duration_seconds = (timing_policy_parameters ->> 'durationSeconds')::INTEGER
WHERE timing_policy_code = 'FIXED_DURATION'
  AND timing_policy_parameters ? 'durationSeconds';

ALTER TABLE assessment.assessment_versions
    ADD CONSTRAINT assessment_versions_availability_check CHECK (
        available_from IS NULL OR available_until IS NULL OR available_from < available_until
    ),
    ADD CONSTRAINT assessment_versions_attempt_duration_check CHECK (
        attempt_duration_seconds IS NULL OR attempt_duration_seconds > 0
    );

ALTER TABLE assessment.assessment_versions
    DROP CONSTRAINT assessment_versions_status_check,
    ADD CONSTRAINT assessment_versions_status_check CHECK (status IN ('DRAFT', 'PUBLISHED', 'CLOSED', 'RETIRED'));

CREATE OR REPLACE FUNCTION assessment.prevent_published_assessment_version_change()
RETURNS TRIGGER AS $$
BEGIN
    IF TG_OP = 'DELETE' AND OLD.status IN ('PUBLISHED', 'CLOSED', 'RETIRED') THEN
        RAISE EXCEPTION 'published assessment versions are immutable';
    END IF;
    IF TG_OP = 'UPDATE' AND OLD.status = 'PUBLISHED' AND
       (NEW.status NOT IN ('CLOSED', 'RETIRED') OR
        (to_jsonb(NEW) - ARRAY['status', 'updated_at', 'version']) IS DISTINCT FROM
        (to_jsonb(OLD) - ARRAY['status', 'updated_at', 'version'])) THEN
        RAISE EXCEPTION 'published assessment versions are immutable';
    END IF;
    IF TG_OP = 'UPDATE' AND OLD.status IN ('CLOSED', 'RETIRED') THEN
        RAISE EXCEPTION 'published assessment versions are immutable';
    END IF;
    RETURN COALESCE(NEW, OLD);
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION assessment.prevent_published_assessment_manifest_change()
RETURNS TRIGGER AS $$
BEGIN
    IF EXISTS (SELECT 1 FROM assessment.assessment_versions WHERE id = COALESCE(NEW.assessment_version_id, OLD.assessment_version_id) AND status IN ('PUBLISHED', 'CLOSED', 'RETIRED')) THEN
        RAISE EXCEPTION 'published assessment version manifest is immutable';
    END IF;
    RETURN COALESCE(NEW, OLD);
END;
$$ LANGUAGE plpgsql;
