package com.mockarena.question.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity @Table(name = "questions", schema = "question")
public class Question {
    @Id private UUID id;
    @Column(nullable = false) private UUID ownerUserId;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private QuestionStatus lifecycleStatus;
    private UUID currentVersionId;
    @Version private long version;
    @Column(nullable = false, updatable = false) private Instant createdAt;
    @Column(nullable = false) private Instant updatedAt;
    protected Question() { }
    public Question(UUID id, UUID ownerUserId, Instant now) { this.id=id; this.ownerUserId=ownerUserId; lifecycleStatus=QuestionStatus.DRAFT; createdAt=now; updatedAt=now; }
    public void setCurrentVersion(UUID id, QuestionStatus status, Instant now) { currentVersionId=id; lifecycleStatus=status; updatedAt=now; }
    public void touch(Instant now) { updatedAt=now; }
    public UUID id() { return id; } public UUID ownerUserId() { return ownerUserId; } public QuestionStatus lifecycleStatus() { return lifecycleStatus; } public UUID currentVersionId() { return currentVersionId; } public long version() { return version; }
    @Override public String toString() { return "Question[id=" + id + ", ownerUserId=" + ownerUserId + ", lifecycleStatus=" + lifecycleStatus + "]"; }
}
