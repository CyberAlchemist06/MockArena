package com.mockarena.challenge.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Entity @Table(name = "challenge_versions", schema = "challenge", uniqueConstraints = @UniqueConstraint(name = "challenge_versions_number_unique", columnNames = {"challenge_id", "version_number"}))
public class ChallengeVersion {
    @Id private UUID id;
    @Column(nullable = false) private UUID challengeId;
    @Column(nullable = false) private int versionNumber;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private ChallengeVersionStatus status;
    @Column(nullable = false) private String title;
    @JdbcTypeCode(SqlTypes.JSON) @Column(nullable = false, columnDefinition = "jsonb") private List<String> tagsAll;
    @JdbcTypeCode(SqlTypes.JSON) @Column(nullable = false, columnDefinition = "jsonb") private List<String> difficulties;
    private String supportedLanguage;
    @Column(nullable = false) private int requestedQuestionCount;
    @Column(nullable = false) private UUID selectionSeed;
    @Version private long version;
    @Column(nullable = false, updatable = false) private Instant createdAt;
    @Column(nullable = false) private Instant updatedAt;
    protected ChallengeVersion() { }
    public ChallengeVersion(UUID id, UUID challengeId, String title, RuleBasedSelection rule, UUID selectionSeed, Instant now) {
        this.id = id; this.challengeId = challengeId; versionNumber = 1; status = ChallengeVersionStatus.DRAFT; this.title = title;
        tagsAll = List.copyOf(rule.tagsAll()); difficulties = List.copyOf(rule.difficulties()); supportedLanguage = rule.supportedLanguage(); requestedQuestionCount = rule.requestedQuestionCount(); this.selectionSeed = selectionSeed; createdAt = now; updatedAt = now;
    }
    public UUID id() { return id; } public UUID challengeId() { return challengeId; } public int versionNumber() { return versionNumber; } public ChallengeVersionStatus status() { return status; } public String title() { return title; } public List<String> tagsAll() { return tagsAll; } public List<String> difficulties() { return difficulties; } public String supportedLanguage() { return supportedLanguage; } public int requestedQuestionCount() { return requestedQuestionCount; } public UUID selectionSeed() { return selectionSeed; }
}
