package com.mockarena.assessment.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity @Table(name = "assessments", schema = "assessment")
public class Assessment {
    @Id private UUID id;
    @Column(nullable = false, updatable = false) private UUID createdByUserId;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private AssessmentVisibility visibility;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private AssessmentStatus lifecycleStatus;
    private UUID currentPublishedVersionId;
    @Version private long version;
    @Column(nullable = false, updatable = false) private Instant createdAt;
    @Column(nullable = false) private Instant updatedAt;
    protected Assessment() { }
    public Assessment(UUID id, UUID createdByUserId, AssessmentVisibility visibility, Instant now) { this.id = id; this.createdByUserId = createdByUserId; this.visibility = visibility; lifecycleStatus = AssessmentStatus.DRAFT; createdAt = now; updatedAt = now; }
    public void publish(UUID versionId, Instant now) { currentPublishedVersionId = versionId; lifecycleStatus = AssessmentStatus.PUBLISHED; updatedAt = now; }
    public void clearCurrentPublishedVersion(UUID versionId, Instant now) { if (versionId.equals(currentPublishedVersionId)) { currentPublishedVersionId = null; updatedAt = now; } }
    public void close(Instant now) { if (lifecycleStatus != AssessmentStatus.PUBLISHED) throw new IllegalStateException("Only published assessments can be closed"); lifecycleStatus = AssessmentStatus.CLOSED; updatedAt = now; }
    public void touch(Instant now) { updatedAt = now; }
    public UUID id() { return id; } public UUID createdByUserId() { return createdByUserId; } public AssessmentVisibility visibility() { return visibility; } public AssessmentStatus lifecycleStatus() { return lifecycleStatus; } public UUID currentPublishedVersionId() { return currentPublishedVersionId; } public long version() { return version; }
}
