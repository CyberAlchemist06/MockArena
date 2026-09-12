ALTER TABLE question.question_versions
    ADD COLUMN question_type_code VARCHAR(64) NOT NULL DEFAULT 'CODING',
    ADD COLUMN content_locale VARCHAR(35) NOT NULL DEFAULT 'en',
    ADD COLUMN difficulty_scheme VARCHAR(64),
    ADD COLUMN difficulty_code VARCHAR(64),
    ADD COLUMN programming_languages JSONB NOT NULL DEFAULT '[]'::jsonb,
    ADD COLUMN default_scoring_policy JSONB NOT NULL DEFAULT '{}'::jsonb;

-- These are legacy V1 projection fields.  Generic metadata is now authoritative;
-- retaining the old columns keeps the V1 catalog contract usable during migration.
ALTER TABLE question.question_versions
    DROP CONSTRAINT question_versions_question_type_check,
    ALTER COLUMN question_type DROP NOT NULL,
    ALTER COLUMN difficulty DROP NOT NULL;

UPDATE question.question_versions
   SET question_type_code = question_type,
       difficulty_scheme = 'mockarena-v1',
       difficulty_code = difficulty,
       programming_languages = supported_languages,
       default_scoring_policy = CASE
           WHEN question_type = 'CODING' THEN jsonb_build_object('policyCode', 'TEST_CASES', 'parameters', scoring_rules)
           ELSE jsonb_build_object('policyCode', 'FIXED_RESPONSE', 'parameters', jsonb_build_object('correctPoints', 1, 'incorrectPoints', 0, 'unansweredPoints', 0))
       END;

CREATE TABLE question.question_version_taxonomy (
    question_version_id UUID NOT NULL REFERENCES question.question_versions(id),
    scheme VARCHAR(64) NOT NULL,
    code VARCHAR(128) NOT NULL,
    PRIMARY KEY (question_version_id, scheme, code)
);

INSERT INTO question.question_version_taxonomy (question_version_id, scheme, code)
SELECT id, 'content-domain', 'dsa' FROM question.question_versions;

INSERT INTO question.question_version_taxonomy (question_version_id, scheme, code)
SELECT qv.id, 'topic', tag
  FROM question.question_versions qv
 CROSS JOIN LATERAL jsonb_array_elements_text(qv.tags) AS tag;

CREATE INDEX question_version_taxonomy_lookup_idx
    ON question.question_version_taxonomy (scheme, code, question_version_id);

CREATE OR REPLACE FUNCTION question.prevent_published_question_version_taxonomy_change()
RETURNS TRIGGER AS $$
BEGIN
    IF EXISTS (SELECT 1 FROM question.question_versions WHERE id = COALESCE(NEW.question_version_id, OLD.question_version_id) AND status = 'PUBLISHED') THEN
        RAISE EXCEPTION 'published question version taxonomy is immutable';
    END IF;
    RETURN COALESCE(NEW, OLD);
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER question_version_taxonomy_immutable_after_publication
BEFORE INSERT OR UPDATE OR DELETE ON question.question_version_taxonomy
FOR EACH ROW EXECUTE FUNCTION question.prevent_published_question_version_taxonomy_change();
