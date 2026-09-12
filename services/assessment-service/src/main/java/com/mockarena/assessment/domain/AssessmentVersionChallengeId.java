package com.mockarena.assessment.domain;
import java.io.Serializable;
import java.util.UUID;
public class AssessmentVersionChallengeId implements Serializable { public UUID assessmentVersionId; public int position; public AssessmentVersionChallengeId() { } public AssessmentVersionChallengeId(UUID assessmentVersionId, int position) { this.assessmentVersionId = assessmentVersionId; this.position = position; } }
