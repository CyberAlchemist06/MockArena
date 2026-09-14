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
    @JdbcTypeCode(SqlTypes.JSON) @Column(nullable = false, columnDefinition = "jsonb") private List<TaxonomyAssignment> taxonomyAll;
    @JdbcTypeCode(SqlTypes.JSON) @Column(nullable = false, columnDefinition = "jsonb") private List<String> questionTypeCodes;
    @JdbcTypeCode(SqlTypes.JSON) @Column(nullable = false, columnDefinition = "jsonb") private List<DifficultyProfile> difficultyProfiles;
    @JdbcTypeCode(SqlTypes.JSON) @Column(nullable = false, columnDefinition = "jsonb") private List<String> contentLocales;
    @JdbcTypeCode(SqlTypes.JSON) @Column(nullable = false, columnDefinition = "jsonb") private List<String> programmingLanguages;
    @Column(nullable = false) private int requestedQuestionCount;
    @JdbcTypeCode(SqlTypes.JSON) @Column(nullable = false, columnDefinition = "jsonb") private List<SelectionGroup> selectionGroups = List.of();
    @Column(nullable = false) private UUID selectionSeed;
    @Version private long version;
    @Column(nullable = false, updatable = false) private Instant createdAt;
    @Column(nullable = false) private Instant updatedAt;
    protected ChallengeVersion() { }
    public ChallengeVersion(UUID id, UUID challengeId, String title, RuleBasedSelection rule, UUID selectionSeed, Instant now) {
        this.id = id; this.challengeId = challengeId; versionNumber = 1; status = ChallengeVersionStatus.DRAFT; this.title = title;
        taxonomyAll = List.copyOf(rule.taxonomyAll()); questionTypeCodes = List.copyOf(rule.questionTypeCodes()); difficultyProfiles = List.copyOf(rule.difficultyProfiles()); contentLocales = List.copyOf(rule.contentLocales()); programmingLanguages = List.copyOf(rule.programmingLanguages()); requestedQuestionCount = rule.requestedQuestionCount(); this.selectionSeed = selectionSeed; createdAt = now; updatedAt = now;
    }
    public ChallengeVersion(UUID id, UUID challengeId, String title, List<SelectionGroup> groups, UUID selectionSeed, Instant now) {
        this.id = id; this.challengeId = challengeId; versionNumber = 1; status = ChallengeVersionStatus.DRAFT; this.title = title;
        taxonomyAll = List.of(); questionTypeCodes = List.of(); difficultyProfiles = List.of(); contentLocales = List.of(); programmingLanguages = List.of();
        selectionGroups = List.copyOf(groups); requestedQuestionCount = groups.stream().mapToInt(SelectionGroup::requestedQuestionCount).sum(); this.selectionSeed = selectionSeed; createdAt = now; updatedAt = now;
    }
    public void publish(Instant now) { if (status != ChallengeVersionStatus.DRAFT) throw new IllegalStateException("Only draft challenge versions can be published"); status = ChallengeVersionStatus.PUBLISHED; updatedAt = now; }
    public void retire(Instant now) { if (status != ChallengeVersionStatus.PUBLISHED) throw new IllegalStateException("Only published challenge versions can be retired"); status = ChallengeVersionStatus.RETIRED; updatedAt = now; }
    public UUID id() { return id; } public UUID challengeId() { return challengeId; } public int versionNumber() { return versionNumber; } public ChallengeVersionStatus status() { return status; } public String title() { return title; } public List<TaxonomyAssignment> taxonomyAll() { return taxonomyAll; } public List<String> questionTypeCodes() { return questionTypeCodes; } public List<DifficultyProfile> difficultyProfiles() { return difficultyProfiles; } public List<String> contentLocales() { return contentLocales; } public List<String> programmingLanguages() { return programmingLanguages; } public int requestedQuestionCount() { return requestedQuestionCount; } public List<SelectionGroup> selectionGroups() { return selectionGroups; } public UUID selectionSeed() { return selectionSeed; } public long version() { return version; }
}
