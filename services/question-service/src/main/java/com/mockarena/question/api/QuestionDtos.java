package com.mockarena.question.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.mockarena.question.domain.Question;
import com.mockarena.question.domain.QuestionVersion;
import com.mockarena.question.domain.Difficulty;
import com.mockarena.question.domain.QuestionType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.List;
import java.util.UUID;

public final class QuestionDtos {
    private QuestionDtos() { }
    public record McqOptionRequest(@NotBlank @Size(max=64) String id, @NotBlank @Size(max=500) String text) { }
    public record McqOptionResponse(String id, String text) { }
    public record ContentRequest(@NotBlank @Size(max=200) String title, @NotNull @Size(max=20) List<@NotBlank @Size(max=50) String> tags,
                                 @NotNull Difficulty difficulty, @NotNull QuestionType questionType, @NotBlank String prompt, String constraintsText,
                                 JsonNode examples, JsonNode supportedLanguages, JsonNode visibleTests, JsonNode hiddenTests, JsonNode scoringRules, JsonNode executionLimits,
                                 @Valid List<McqOptionRequest> options, String correctOptionId, String explanation) {
        @Override public String toString() { return "ContentRequest[title=" + title + ", questionType=" + questionType + "]"; }
    }
    public record CreateQuestionRequest(@NotNull @Valid ContentRequest content) { }
    public record UpdateVersionRequest(@Min(0) long expectedVersion, @NotNull @Valid ContentRequest content) { }
    public record CreateRevisionRequest(@Min(0) long expectedQuestionVersion, @NotNull @Valid ContentRequest content) { }
    public record PublishVersionRequest(@Min(0) long expectedQuestionVersion, @Min(0) long expectedVersion) { }
    // The authenticated subject owns the Question; this response deliberately omits hidden tests.
    public record QuestionResponse(UUID id, UUID ownerUserId, String lifecycleStatus, UUID currentVersionId, long version) { }
    // This is the only version representation exposed by this service's HTTP API; protected tests are never mapped into it.
    public record QuestionVersionResponse(UUID id, UUID questionId, int versionNumber, String status, String title, List<String> tags, Difficulty difficulty, QuestionType questionType, String prompt,
                                          String constraintsText, JsonNode examples, JsonNode supportedLanguages, JsonNode visibleTests,
                                          JsonNode scoringRules, JsonNode executionLimits, List<McqOptionResponse> options, long version) { }
    public static QuestionResponse toResponse(Question q) { return new QuestionResponse(q.id(), q.ownerUserId(), q.lifecycleStatus().name(), q.currentVersionId(), q.version()); }
    public static QuestionVersionResponse toResponse(QuestionVersion v) {
        List<McqOptionResponse> options = v.options().stream().map(option -> new McqOptionResponse(option.id(), option.text())).toList();
        return new QuestionVersionResponse(v.id(), v.questionId(), v.versionNumber(), v.status().name(), v.title(), v.tags(), v.difficulty(), v.questionType(), v.prompt(), v.constraintsText(), v.examples(), v.supportedLanguages(), v.visibleTests(), v.scoringRules(), v.executionLimits(), options, v.version());
    }
}
