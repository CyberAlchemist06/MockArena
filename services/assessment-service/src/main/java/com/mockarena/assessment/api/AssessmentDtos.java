package com.mockarena.assessment.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.mockarena.assessment.domain.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.*;

public final class AssessmentDtos {
    private AssessmentDtos() { }
    public record PolicyRequest(@NotBlank @Size(max = 64) String policyCode, @NotNull JsonNode parameters) { }
    public record ContentRequest(@NotBlank @Size(max = 200) String title, @Size(max = 10_000) String description, @Size(max = 20_000) String instructions,
                                 @NotBlank @Size(max = 64) String assessmentTypeCode, @NotNull @Valid PolicyRequest timingPolicy, java.time.Instant availableFrom, java.time.Instant availableUntil, @Min(1) Integer attemptDurationSeconds,
                                 @NotNull @Valid PolicyRequest attemptPolicy, @NotNull @Valid PolicyRequest resultReleasePolicy,
                                 @NotEmpty @Size(max = 100) List<@NotNull UUID> challengeVersionIds) { }
    public record CreateAssessmentRequest(@NotNull AssessmentVisibility visibility, @NotNull @Valid ContentRequest content) { }
    public record UpdateAssessmentVersionRequest(@Min(0) long expectedVersion, @NotNull @Valid ContentRequest content) { }
    public record CreateRevisionRequest(@Min(0) long expectedAssessmentVersion, @NotNull @Valid ContentRequest content) { }
    public record PublishAssessmentVersionRequest(@Min(0) long expectedAssessmentVersion, @Min(0) long expectedVersion) { }
    public record RetireAssessmentVersionRequest(@Min(0) long expectedAssessmentVersion, @Min(0) long expectedVersion) { }
    public record CloseAssessmentRequest(@Min(0) long expectedAssessmentVersion) { }
    public record CloseAssessmentVersionRequest(@Min(0) long expectedAssessmentVersion, @Min(0) long expectedVersion) { }
    public record AttemptResponse(UUID attemptId, UUID assessmentId, UUID assessmentVersionId, int assessmentVersionNumber, String status, java.time.Instant startedAt, java.time.Instant deadlineAt, java.time.Instant expiredAt, long version) { }
    public record SaveAttemptResponseRequest(@NotBlank @Size(max = 64) String responseTypeCode, @Size(max = 200) String selectedOptionId,
                                             @Size(max = 64) String programmingLanguage, @Size(max = 500_000) String sourceCode,
                                             @NotNull @Min(0) Long expectedResponseVersion, @NotNull UUID clientMutationId) {
        @Override public String toString() { return "SaveAttemptResponseRequest[type=" + responseTypeCode + ", expectedResponseVersion=" + expectedResponseVersion + ", clientMutationId=" + clientMutationId + "]"; }
    }
    public record SavedAttemptResponse(UUID attemptId, int globalPosition, String responseTypeCode, String selectedOptionId,
                                       String programmingLanguage, String sourceCode, long version) {
        @Override public String toString() { return "SavedAttemptResponse[attemptId=" + attemptId + ", globalPosition=" + globalPosition + ", responseTypeCode=" + responseTypeCode + ", version=" + version + "]"; }
    }
    public record ChallengeReferenceResponse(int position, UUID challengeId, UUID challengeVersionId, int challengeVersionNumber) { }
    public record AssessmentResponse(UUID assessmentId, UUID createdByUserId, String lifecycleStatus, AssessmentVisibility visibility, UUID currentPublishedVersionId, long version) { }
    public record AssessmentVersionResponse(UUID assessmentVersionId, UUID assessmentId, int versionNumber, String status, String title, String description, String instructions,
                                            String assessmentTypeCode, PolicyRequest timingPolicy, java.time.Instant availableFrom, java.time.Instant availableUntil, Integer attemptDurationSeconds, PolicyRequest attemptPolicy, PolicyRequest resultReleasePolicy,
                                            List<ChallengeReferenceResponse> challenges, long version) { }
    public static AssessmentResponse assessment(Assessment value) { return new AssessmentResponse(value.id(), value.createdByUserId(), value.lifecycleStatus().name(), value.visibility(), value.currentPublishedVersionId(), value.version()); }
    public static AssessmentVersionResponse version(AssessmentVersion value, List<AssessmentVersionChallenge> challenges) {
        return new AssessmentVersionResponse(value.id(), value.assessmentId(), value.versionNumber(), value.status().name(), value.title(), value.description(), value.instructions(), value.assessmentTypeCode(),
            new PolicyRequest(value.timingPolicyCode(), value.timingPolicyParameters()), value.availableFrom(), value.availableUntil(), value.attemptDurationSeconds(), new PolicyRequest(value.attemptPolicyCode(), value.attemptPolicyParameters()), new PolicyRequest(value.resultReleasePolicyCode(), value.resultReleasePolicyParameters()),
            challenges.stream().map(reference -> new ChallengeReferenceResponse(reference.position(), reference.challengeId(), reference.challengeVersionId(), reference.challengeVersionNumber())).toList(), value.version());
    }
    public static AttemptResponse attempt(com.mockarena.assessment.domain.Attempt value) { return new AttemptResponse(value.id(), value.assessmentId(), value.assessmentVersionId(), value.assessmentVersionNumber(), value.status().name(), value.startedAt(), value.deadlineAt(), value.expiredAt(), value.version()); }
}
