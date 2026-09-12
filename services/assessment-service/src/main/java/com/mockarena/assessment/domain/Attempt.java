package com.mockarena.assessment.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity @Table(name = "attempts", schema = "assessment")
public class Attempt {
    @Id private UUID id;
    @Column(nullable = false, updatable = false) private UUID assessmentId;
    @Column(nullable = false, updatable = false) private UUID assessmentVersionId;
    @Column(nullable = false, updatable = false) private int assessmentVersionNumber;
    @Column(nullable = false, updatable = false) private UUID candidateUserId;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private AttemptStatus status;
    @Column(nullable = false, updatable = false) private Instant startedAt;
    private Instant deadlineAt; private Instant submittedAt; private Instant completedAt; private Instant expiredAt;
    @Column(updatable = false) private UUID entitlementReservationId;
    @Version private long version;
    @Column(nullable = false, updatable = false) private Instant createdAt;
    @Column(nullable = false) private Instant updatedAt;
    protected Attempt() { }
    public Attempt(UUID id, UUID assessmentId, UUID assessmentVersionId, int assessmentVersionNumber, UUID candidateUserId, Instant startedAt, Instant deadlineAt, UUID entitlementReservationId) {
        this.id = id; this.assessmentId = assessmentId; this.assessmentVersionId = assessmentVersionId; this.assessmentVersionNumber = assessmentVersionNumber; this.candidateUserId = candidateUserId;
        status = AttemptStatus.IN_PROGRESS; this.startedAt = startedAt; this.deadlineAt = deadlineAt; this.entitlementReservationId = entitlementReservationId; createdAt = startedAt; updatedAt = startedAt;
    }
    public boolean expireIfDue(Instant now) { if (status == AttemptStatus.IN_PROGRESS && deadlineAt != null && deadlineAt.isBefore(now)) { status = AttemptStatus.EXPIRED; expiredAt = now; updatedAt = now; return true; } return false; }
    public UUID id() { return id; } public UUID assessmentId() { return assessmentId; } public UUID assessmentVersionId() { return assessmentVersionId; } public int assessmentVersionNumber() { return assessmentVersionNumber; } public UUID candidateUserId() { return candidateUserId; } public AttemptStatus status() { return status; } public Instant startedAt() { return startedAt; } public Instant deadlineAt() { return deadlineAt; } public Instant submittedAt() { return submittedAt; } public Instant completedAt() { return completedAt; } public Instant expiredAt() { return expiredAt; } public UUID entitlementReservationId() { return entitlementReservationId; } public long version() { return version; }
}
