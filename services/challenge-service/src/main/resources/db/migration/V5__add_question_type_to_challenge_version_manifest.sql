-- Challenge owns the immutable ID-only manifest.  The type code is safe metadata
-- needed by Assessment Service to route a future attempt without fetching content.
ALTER TABLE challenge.challenge_version_questions
    ADD COLUMN question_type_code VARCHAR(64) NOT NULL DEFAULT 'LEGACY_UNSPECIFIED',
    ADD CONSTRAINT challenge_version_questions_type_code_check CHECK (length(trim(question_type_code)) > 0);
