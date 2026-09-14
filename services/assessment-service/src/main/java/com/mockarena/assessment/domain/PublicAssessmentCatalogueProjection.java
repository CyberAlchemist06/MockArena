package com.mockarena.assessment.domain;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.Instant;
import java.util.UUID;

@Entity @Table(name = "public_assessment_catalogue", schema = "assessment")
public class PublicAssessmentCatalogueProjection {
    @Id private UUID assessmentVersionId;
    @Column(nullable = false) private UUID assessmentId;
    @Column(nullable = false) private int versionNumber;
    @Column(nullable = false) private String title;
    @Column(columnDefinition = "text") private String description;
    private String instructionsSummary;
    @Column(nullable = false) private String assessmentTypeCode;
    @Column(nullable = false) private String timingPolicyCode;
    private Integer attemptDurationSeconds;
    private Instant availableFrom;
    private Instant availableUntil;
    @Column(nullable = false) private int maxAttempts;
    @Column(nullable = false) private String resultReleasePolicyCode;
    private Instant resultReleaseAt;
    @Column(nullable = false) private int questionCount;
    @JdbcTypeCode(SqlTypes.JSON) @Column(nullable = false, columnDefinition = "jsonb") private JsonNode questionTypeCounts;
    @Column(nullable = false) private Instant publishedAt;
    protected PublicAssessmentCatalogueProjection() { }
    public PublicAssessmentCatalogueProjection(UUID assessmentVersionId, UUID assessmentId, int versionNumber, String title, String description, String instructionsSummary, String assessmentTypeCode, String timingPolicyCode, Integer attemptDurationSeconds, Instant availableFrom, Instant availableUntil, int maxAttempts, String resultReleasePolicyCode, Instant resultReleaseAt, int questionCount, JsonNode questionTypeCounts, Instant publishedAt) {
        this.assessmentVersionId = assessmentVersionId; this.assessmentId = assessmentId; this.versionNumber = versionNumber; this.title = title; this.description = description; this.instructionsSummary = instructionsSummary; this.assessmentTypeCode = assessmentTypeCode; this.timingPolicyCode = timingPolicyCode; this.attemptDurationSeconds = attemptDurationSeconds; this.availableFrom = availableFrom; this.availableUntil = availableUntil; this.maxAttempts = maxAttempts; this.resultReleasePolicyCode = resultReleasePolicyCode; this.resultReleaseAt = resultReleaseAt; this.questionCount = questionCount; this.questionTypeCounts = questionTypeCounts; this.publishedAt = publishedAt;
    }
}
