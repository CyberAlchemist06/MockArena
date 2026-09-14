-- Published and retired QuestionVersions are immutable historical records.  Their
-- generic metadata is therefore projected into this additive snapshot rather than
-- being written back to question.question_versions.
CREATE TABLE question.question_version_historical_generic_metadata (
    question_version_id UUID PRIMARY KEY REFERENCES question.question_versions(id),
    question_type_code VARCHAR(64) NOT NULL,
    content_locale VARCHAR(35) NOT NULL,
    difficulty_scheme VARCHAR(64),
    difficulty_code VARCHAR(64),
    programming_languages JSONB NOT NULL,
    default_scoring_policy JSONB NOT NULL
);

INSERT INTO question.question_version_historical_generic_metadata (
    question_version_id,
    question_type_code,
    content_locale,
    difficulty_scheme,
    difficulty_code,
    programming_languages,
    default_scoring_policy
)
SELECT id,
       question_type,
       'en',
       'mockarena-v1',
       difficulty,
       COALESCE(supported_languages, '[]'::jsonb),
       CASE
           WHEN question_type = 'CODING' THEN jsonb_build_object('policyCode', 'TEST_CASES', 'parameters', scoring_rules)
           ELSE jsonb_build_object('policyCode', 'FIXED_RESPONSE', 'parameters', jsonb_build_object('correctPoints', 1, 'incorrectPoints', 0, 'unansweredPoints', 0))
       END
  FROM question.question_versions
 WHERE status IN ('PUBLISHED', 'RETIRED');

CREATE OR REPLACE FUNCTION question.prevent_historical_question_version_generic_metadata_change()
RETURNS TRIGGER AS $$
BEGIN
    IF EXISTS (
        SELECT 1
          FROM question.question_versions
         WHERE id = COALESCE(NEW.question_version_id, OLD.question_version_id)
           AND status IN ('PUBLISHED', 'RETIRED')
    ) THEN
        RAISE EXCEPTION 'historical question version generic metadata is immutable';
    END IF;
    RETURN COALESCE(NEW, OLD);
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER question_version_historical_generic_metadata_immutable
BEFORE INSERT OR UPDATE OR DELETE ON question.question_version_historical_generic_metadata
FOR EACH ROW EXECUTE FUNCTION question.prevent_historical_question_version_generic_metadata_change();
