package com.mockarena.assessment.domain;

import jakarta.persistence.*;
import java.util.UUID;

@Entity @IdClass(AssessmentVersionChallengeId.class) @Table(name = "assessment_version_challenges", schema = "assessment")
public class AssessmentVersionChallenge {
    @Id @Column(nullable = false) private UUID assessmentVersionId;
    @Id @Column(nullable = false) private int position;
    @Column(nullable = false) private UUID challengeId;
    @Column(nullable = false) private UUID challengeVersionId;
    @Column(nullable = false) private int challengeVersionNumber;
    protected AssessmentVersionChallenge() { }
    public AssessmentVersionChallenge(UUID assessmentVersionId, int position, UUID challengeId, UUID challengeVersionId, int challengeVersionNumber) { this.assessmentVersionId = assessmentVersionId; this.position = position; this.challengeId = challengeId; this.challengeVersionId = challengeVersionId; this.challengeVersionNumber = challengeVersionNumber; }
    public UUID assessmentVersionId() { return assessmentVersionId; } public int position() { return position; } public UUID challengeId() { return challengeId; } public UUID challengeVersionId() { return challengeVersionId; } public int challengeVersionNumber() { return challengeVersionNumber; }
}
