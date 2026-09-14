package com.mockarena.assessment.domain;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.Instant;
import java.util.UUID;

@Entity @Table(name = "assessment_versions", schema = "assessment", uniqueConstraints = @UniqueConstraint(name = "assessment_versions_number_unique", columnNames = {"assessment_id", "version_number"}))
public class AssessmentVersion {
    @Id private UUID id;
    @Column(nullable = false) private UUID assessmentId;
    @Column(nullable = false) private int versionNumber;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private AssessmentVersionStatus status;
    @Column(nullable = false) private String title;
    @Column(columnDefinition = "text") private String description;
    @Column(columnDefinition = "text") private String instructions;
    @Column(nullable = false) private String assessmentTypeCode;
    @Column(nullable = false) private String timingPolicyCode;
    @JdbcTypeCode(SqlTypes.JSON) @Column(nullable = false, columnDefinition = "jsonb") private JsonNode timingPolicyParameters;
    private Instant availableFrom;
    private Instant availableUntil;
    private Integer attemptDurationSeconds;
    @Column(nullable = false) private String attemptPolicyCode;
    @JdbcTypeCode(SqlTypes.JSON) @Column(nullable = false, columnDefinition = "jsonb") private JsonNode attemptPolicyParameters;
    @Column(nullable = false) private String resultReleasePolicyCode;
    @JdbcTypeCode(SqlTypes.JSON) @Column(nullable = false, columnDefinition = "jsonb") private JsonNode resultReleasePolicyParameters;
    @Version private long version;
    @Column(nullable = false, updatable = false) private Instant createdAt;
    @Column(nullable = false) private Instant updatedAt;
    protected AssessmentVersion() { }
    public AssessmentVersion(UUID id, UUID assessmentId, int versionNumber, Content content, Instant now) { this.id = id; this.assessmentId = assessmentId; this.versionNumber = versionNumber; status = AssessmentVersionStatus.DRAFT; apply(content, now); createdAt = now; }
    public void update(Content content, Instant now) { if (status != AssessmentVersionStatus.DRAFT) throw new IllegalStateException("Only draft assessment versions can be changed"); apply(content, now); }
    public void publish(Instant now) { if (status != AssessmentVersionStatus.DRAFT) throw new IllegalStateException("Only draft assessment versions can be published"); status = AssessmentVersionStatus.PUBLISHED; updatedAt = now; }
    public void retire(Instant now) { if (status != AssessmentVersionStatus.PUBLISHED) throw new IllegalStateException("Only published assessment versions can be retired"); status = AssessmentVersionStatus.RETIRED; updatedAt = now; }
    public void close(Instant now) { if (status != AssessmentVersionStatus.PUBLISHED) throw new IllegalStateException("Only published assessment versions can be closed"); status = AssessmentVersionStatus.CLOSED; updatedAt = now; }
    private void apply(Content content, Instant now) {
        if (content.title() == null || content.title().isBlank()) throw new IllegalArgumentException("Assessment title is required");
        title = content.title().trim(); description = blankToNull(content.description()); instructions = blankToNull(content.instructions()); assessmentTypeCode = PolicyValidator.assessmentType(content.assessmentTypeCode());
        PolicyValidator.availability(content.availableFrom(), content.availableUntil()); timingPolicyCode = PolicyValidator.timing(content.timingPolicyCode(), content.timingPolicyParameters(), content.attemptDurationSeconds()); timingPolicyParameters = content.timingPolicyParameters().deepCopy(); availableFrom = content.availableFrom(); availableUntil = content.availableUntil(); attemptDurationSeconds = content.attemptDurationSeconds();
        attemptPolicyCode = PolicyValidator.attempt(content.attemptPolicyCode(), content.attemptPolicyParameters()); attemptPolicyParameters = content.attemptPolicyParameters().deepCopy();
        resultReleasePolicyCode = PolicyValidator.release(content.resultReleasePolicyCode(), content.resultReleasePolicyParameters()); resultReleasePolicyParameters = content.resultReleasePolicyParameters().deepCopy(); updatedAt = now;
    }
    private static String blankToNull(String value) { return value == null || value.isBlank() ? null : value.trim(); }
    public boolean canStartAttemptAt(Instant now) { return status == AssessmentVersionStatus.PUBLISHED && (availableFrom == null || !now.isBefore(availableFrom)) && (availableUntil == null || now.isBefore(availableUntil)); }
    public Instant effectiveAttemptDeadline(Instant startedAt) { Instant durationDeadline = attemptDurationSeconds == null ? null : startedAt.plusSeconds(attemptDurationSeconds); if (durationDeadline == null) return availableUntil; if (availableUntil == null) return durationDeadline; return durationDeadline.isBefore(availableUntil) ? durationDeadline : availableUntil; }
    public UUID id() { return id; } public UUID assessmentId() { return assessmentId; } public int versionNumber() { return versionNumber; } public AssessmentVersionStatus status() { return status; } public String title() { return title; } public String description() { return description; } public String instructions() { return instructions; } public String assessmentTypeCode() { return assessmentTypeCode; } public String timingPolicyCode() { return timingPolicyCode; } public JsonNode timingPolicyParameters() { return timingPolicyParameters; } public Instant availableFrom() { return availableFrom; } public Instant availableUntil() { return availableUntil; } public Integer attemptDurationSeconds() { return attemptDurationSeconds; } public String attemptPolicyCode() { return attemptPolicyCode; } public JsonNode attemptPolicyParameters() { return attemptPolicyParameters; } public String resultReleasePolicyCode() { return resultReleasePolicyCode; } public JsonNode resultReleasePolicyParameters() { return resultReleasePolicyParameters; } public long version() { return version; } public Instant updatedAt() { return updatedAt; }
    public record Content(String title, String description, String instructions, String assessmentTypeCode, String timingPolicyCode, JsonNode timingPolicyParameters, Instant availableFrom, Instant availableUntil, Integer attemptDurationSeconds, String attemptPolicyCode, JsonNode attemptPolicyParameters, String resultReleasePolicyCode, JsonNode resultReleasePolicyParameters) { }
}
