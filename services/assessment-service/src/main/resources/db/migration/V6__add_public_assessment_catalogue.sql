CREATE TABLE assessment.public_assessment_catalogue (
    assessment_version_id UUID PRIMARY KEY REFERENCES assessment.assessment_versions(id),
    assessment_id UUID NOT NULL REFERENCES assessment.assessments(id),
    version_number INTEGER NOT NULL,
    title VARCHAR(200) NOT NULL,
    description TEXT,
    instructions_summary VARCHAR(1000),
    assessment_type_code VARCHAR(64) NOT NULL,
    timing_policy_code VARCHAR(64) NOT NULL,
    attempt_duration_seconds INTEGER,
    available_from TIMESTAMPTZ,
    available_until TIMESTAMPTZ,
    max_attempts INTEGER NOT NULL,
    result_release_policy_code VARCHAR(64) NOT NULL,
    result_release_at TIMESTAMPTZ,
    question_count INTEGER NOT NULL,
    question_type_counts JSONB NOT NULL,
    published_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT public_assessment_catalogue_question_count_check CHECK (question_count > 0),
    CONSTRAINT public_assessment_catalogue_question_type_counts_object_check CHECK (jsonb_typeof(question_type_counts) = 'object')
);

CREATE INDEX public_assessment_catalogue_published_idx
    ON assessment.public_assessment_catalogue (published_at DESC, assessment_id ASC);
CREATE INDEX public_assessment_catalogue_type_published_idx
    ON assessment.public_assessment_catalogue (assessment_type_code, published_at DESC, assessment_id ASC);
