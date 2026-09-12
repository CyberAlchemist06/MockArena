package com.mockarena.assessment.domain;

import jakarta.persistence.*; import java.time.Instant; import java.util.UUID;
@Entity @Table(name = "attempt_start_requests", schema = "assessment")
public class AttemptStartRequest {
    @Id private UUID id; @Column(nullable = false) private UUID candidateUserId; @Column(nullable = false) private String idempotencyKey; @Column(nullable = false) private String requestFingerprint;
    @Column(nullable = false) private UUID assessmentId; @Column(nullable = false) private UUID assessmentVersionId; @Column(nullable = false) private UUID attemptId; @Column(nullable = false) private String state;
    @Column(nullable = false) private Instant createdAt; @Column(nullable = false) private Instant updatedAt;
    protected AttemptStartRequest() { }
    public AttemptStartRequest(UUID id, UUID candidateUserId, String key, String fingerprint, UUID assessmentId, UUID assessmentVersionId, UUID attemptId, String state, Instant now) { this.id=id; this.candidateUserId=candidateUserId; idempotencyKey=key; requestFingerprint=fingerprint; this.assessmentId=assessmentId; this.assessmentVersionId=assessmentVersionId; this.attemptId=attemptId; this.state=state; createdAt=now; updatedAt=now; }
    public UUID attemptId() { return attemptId; } public String requestFingerprint() { return requestFingerprint; } public UUID assessmentId() { return assessmentId; } public UUID assessmentVersionId() { return assessmentVersionId; }
}
