package com.mockarena.assessment.domain;

import jakarta.persistence.*;
import java.util.UUID;

@Entity @IdClass(AttemptItemId.class) @Table(name = "attempt_items", schema = "assessment")
public class AttemptItem {
    @Id @Column(nullable = false) private UUID attemptId;
    @Id @Column(nullable = false) private int globalPosition;
    @Column(nullable = false) private UUID challengeId; @Column(nullable = false) private UUID challengeVersionId; @Column(nullable = false) private int challengePosition;
    @Column(nullable = false) private UUID questionId; @Column(nullable = false) private UUID questionVersionId; @Column(nullable = false) private int questionPosition; @Column(nullable = false) private String questionTypeCode;
    protected AttemptItem() { }
    public AttemptItem(UUID attemptId, int globalPosition, UUID challengeId, UUID challengeVersionId, int challengePosition, UUID questionId, UUID questionVersionId, int questionPosition, String questionTypeCode) { this.attemptId = attemptId; this.globalPosition = globalPosition; this.challengeId = challengeId; this.challengeVersionId = challengeVersionId; this.challengePosition = challengePosition; this.questionId = questionId; this.questionVersionId = questionVersionId; this.questionPosition = questionPosition; this.questionTypeCode = questionTypeCode; }
    public UUID attemptId() { return attemptId; } public int globalPosition() { return globalPosition; } public UUID challengeId() { return challengeId; } public UUID challengeVersionId() { return challengeVersionId; } public int challengePosition() { return challengePosition; } public UUID questionId() { return questionId; } public UUID questionVersionId() { return questionVersionId; } public int questionPosition() { return questionPosition; } public String questionTypeCode() { return questionTypeCode; }
}
