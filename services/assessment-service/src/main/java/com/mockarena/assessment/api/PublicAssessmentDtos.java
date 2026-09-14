package com.mockarena.assessment.api;

import java.time.Instant;
import java.util.*;

public final class PublicAssessmentDtos {
    private PublicAssessmentDtos() { }
    public record TimingSummary(String policyCode, Integer attemptDurationSeconds) { }
    public record AvailabilitySummary(Instant availableFrom, Instant availableUntil) { }
    public record AttemptPolicySummary(String policyCode, int maxAttempts) { }
    public record ResultReleaseSummary(String policyCode, Instant releaseAt) { }
    public record AssessmentSummary(UUID assessmentId, int versionNumber, String title, String description, String assessmentTypeCode, String visibility, TimingSummary timing, AvailabilitySummary availability, int questionCount, Map<String, Integer> questionTypeCounts) { }
    public record AssessmentDetail(UUID assessmentId, int versionNumber, String title, String description, String instructionsSummary, String assessmentTypeCode, String visibility, TimingSummary timing, AvailabilitySummary availability, AttemptPolicySummary attemptPolicy, ResultReleaseSummary resultRelease, int questionCount, Map<String, Integer> questionTypeCounts) { }
    public record CataloguePage(List<AssessmentSummary> items, String nextCursor) { }
}
