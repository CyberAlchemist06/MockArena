package com.mockarena.question.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.mockarena.question.domain.Question;
import com.mockarena.question.domain.QuestionVersion;
import com.mockarena.question.domain.Difficulty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.List;
import java.util.UUID;

public final class QuestionDtos {
    private QuestionDtos() { }
    public record ContentRequest(@NotBlank @Size(max=200) String title, @NotNull @Size(max=20) List<@NotBlank @Size(max=50) String> tags,
                                 @NotNull Difficulty difficulty, @NotBlank String prompt, String constraintsText,
                                 @NotNull JsonNode examples, @NotNull JsonNode supportedLanguages, @NotNull JsonNode visibleTests,
                                 @NotNull JsonNode hiddenTests, @NotNull JsonNode scoringRules, @NotNull JsonNode executionLimits) { }
    public record CreateQuestionRequest(@NotNull UUID ownerUserId, @NotNull @Valid ContentRequest content) { }
    public record UpdateVersionRequest(@Min(0) long expectedVersion, @NotNull ContentRequest content) { }
    public record PublishVersionRequest(@Min(0) long expectedQuestionVersion, @Min(0) long expectedVersion) { }
    // This DTO deliberately omits hiddenTests. ownerUserId is provenance metadata only; no authorization is implied here.
    public record QuestionResponse(UUID id, UUID ownerUserId, String lifecycleStatus, UUID currentVersionId, long version) { }
    // This is the only version representation exposed by this service's HTTP API; protected tests are never mapped into it.
    public record QuestionVersionResponse(UUID id, UUID questionId, int versionNumber, String status, String title, List<String> tags, Difficulty difficulty, String prompt,
                                          String constraintsText, JsonNode examples, JsonNode supportedLanguages, JsonNode visibleTests,
                                          JsonNode scoringRules, JsonNode executionLimits, long version) { }
    public static QuestionResponse toResponse(Question q) { return new QuestionResponse(q.id(), q.ownerUserId(), q.lifecycleStatus().name(), q.currentVersionId(), q.version()); }
    public static QuestionVersionResponse toResponse(QuestionVersion v) {
        return new QuestionVersionResponse(v.id(), v.questionId(), v.versionNumber(), v.status().name(), v.title(), v.tags(), v.difficulty(), v.prompt(), v.constraintsText(), v.examples(), v.supportedLanguages(), v.visibleTests(), v.scoringRules(), v.executionLimits(), v.version());
    }
}
