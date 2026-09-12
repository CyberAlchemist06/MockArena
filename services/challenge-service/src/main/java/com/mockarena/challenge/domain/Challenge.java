package com.mockarena.challenge.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity @Table(name = "challenges", schema = "challenge")
public class Challenge {
    @Id private UUID id;
    private UUID createdByUserId;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private ChallengeVisibility visibility;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private ChallengeStatus lifecycleStatus;
    private UUID currentPublishedVersionId;
    @Version private long version;
    @Column(nullable = false, updatable = false) private Instant createdAt;
    @Column(nullable = false) private Instant updatedAt;
    protected Challenge() { }
    public Challenge(UUID id, UUID createdByUserId, ChallengeVisibility visibility, Instant now) { this.id = id; this.createdByUserId = createdByUserId; this.visibility = visibility; lifecycleStatus = ChallengeStatus.DRAFT; createdAt = now; updatedAt = now; }
    public void publish(UUID versionId, Instant now) { currentPublishedVersionId = versionId; lifecycleStatus = ChallengeStatus.PUBLISHED; updatedAt = now; }
    public void clearCurrentPublishedVersion(UUID versionId, Instant now) { if (versionId.equals(currentPublishedVersionId)) { currentPublishedVersionId = null; updatedAt = now; } }
    public UUID id() { return id; } public UUID createdByUserId() { return createdByUserId; } public ChallengeVisibility visibility() { return visibility; } public ChallengeStatus lifecycleStatus() { return lifecycleStatus; } public UUID currentPublishedVersionId() { return currentPublishedVersionId; } public long version() { return version; }
}
