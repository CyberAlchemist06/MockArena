ALTER TABLE challenge.challenge_versions
    ADD COLUMN taxonomy_all JSONB NOT NULL DEFAULT '[]'::jsonb,
    ADD COLUMN question_type_codes JSONB NOT NULL DEFAULT '[]'::jsonb,
    ADD COLUMN difficulty_profiles JSONB NOT NULL DEFAULT '[]'::jsonb,
    ADD COLUMN content_locales JSONB NOT NULL DEFAULT '[]'::jsonb,
    ADD COLUMN programming_languages JSONB NOT NULL DEFAULT '[]'::jsonb;

-- Convert mutable V1 drafts only. Published and retired manifests remain historical data.
UPDATE challenge.challenge_versions cv
   SET taxonomy_all = (
           SELECT COALESCE(jsonb_agg(DISTINCT jsonb_build_object('scheme', 'topic', 'code', tag)), '[]'::jsonb)
             FROM jsonb_array_elements_text(cv.tags_all) AS tag
       ),
       difficulty_profiles = (
           SELECT COALESCE(jsonb_agg(DISTINCT jsonb_build_object('scheme', 'mockarena-v1', 'code', difficulty)), '[]'::jsonb)
             FROM jsonb_array_elements_text(cv.difficulties) AS difficulty
       ),
       programming_languages = CASE WHEN cv.supported_language IS NULL THEN '[]'::jsonb
                                    ELSE jsonb_build_array(cv.supported_language) END
 WHERE cv.status = 'DRAFT';
