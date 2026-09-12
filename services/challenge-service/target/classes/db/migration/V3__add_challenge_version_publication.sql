ALTER TABLE challenge.challenges
    ADD COLUMN current_published_version_id UUID;

CREATE INDEX challenges_current_published_version_id_idx
    ON challenge.challenges (current_published_version_id);

-- Keep the V1 immutability triggers, while allowing the one legal lifecycle
-- transition from PUBLISHED to RETIRED without permitting content changes.
CREATE OR REPLACE FUNCTION challenge.prevent_published_challenge_version_change()
RETURNS TRIGGER AS $$
BEGIN
    IF TG_OP = 'DELETE' AND OLD.status IN ('PUBLISHED', 'RETIRED') THEN
        RAISE EXCEPTION 'published challenge versions are immutable';
    END IF;
    IF TG_OP = 'UPDATE' AND OLD.status = 'PUBLISHED' AND
       (NEW.status <> 'RETIRED' OR
        (to_jsonb(NEW) - ARRAY['status', 'updated_at', 'version']) IS DISTINCT FROM
        (to_jsonb(OLD) - ARRAY['status', 'updated_at', 'version'])) THEN
        RAISE EXCEPTION 'published challenge versions are immutable';
    END IF;
    IF TG_OP = 'UPDATE' AND OLD.status = 'RETIRED' THEN
        RAISE EXCEPTION 'published challenge versions are immutable';
    END IF;
    RETURN COALESCE(NEW, OLD);
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION challenge.prevent_published_composition_change()
RETURNS TRIGGER AS $$
BEGIN
    IF EXISTS (SELECT 1 FROM challenge.challenge_versions WHERE id = COALESCE(NEW.challenge_version_id, OLD.challenge_version_id) AND status IN ('PUBLISHED', 'RETIRED')) THEN
        RAISE EXCEPTION 'published challenge version composition is immutable';
    END IF;
    RETURN COALESCE(NEW, OLD);
END;
$$ LANGUAGE plpgsql;
