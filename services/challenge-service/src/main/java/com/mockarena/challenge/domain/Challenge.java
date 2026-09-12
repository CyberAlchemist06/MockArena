package com.mockarena.challenge.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity @Table(name = "challenges", schema = "challenge")
public class Challenge {
    @Id private UUID id;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private ChallengeVisibility visibility;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private ChallengeStatus lifecycleStatus;
    @Version private long version;
    @Column(nullable = false, updatable = false) private Instant createdAt;
    @Column(nullable = false) private Instant updatedAt;
    protected Challenge() { }
    public Challenge(UUID id, ChallengeVisibility visibility, Instant now) { this.id = id; this.visibility = visibility; lifecycleStatus = ChallengeStatus.DRAFT; createdAt = now; updatedAt = now; }
    public UUID id() { return id; } public ChallengeVisibility visibility() { return visibility; } public ChallengeStatus lifecycleStatus() { return lifecycleStatus; } public long version() { return version; }
}
