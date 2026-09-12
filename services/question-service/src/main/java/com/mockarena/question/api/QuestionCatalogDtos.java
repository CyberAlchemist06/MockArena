package com.mockarena.question.api;

import com.mockarena.question.application.QuestionVersionCatalogEntry;
import com.mockarena.question.domain.Difficulty;
import com.mockarena.question.domain.QuestionType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;

public final class QuestionCatalogDtos {
    private QuestionCatalogDtos() { }

    public record ResolveQuestionVersionsRequest(
            @Size(max = 20) List<@NotBlank @Size(max = 50) String> tagsAll,
            @Size(max = 3) List<Difficulty> difficulties,
            @Pattern(regexp = "^[A-Za-z][A-Za-z0-9_+.-]{0,31}$") String supportedLanguage,
            @NotNull @Min(1) @Max(100) Integer limit) { }

    public record QuestionVersionCatalogEntryResponse(
            UUID questionId,
            UUID questionVersionId,
            int versionNumber,
            String title,
            List<String> tags,
            Difficulty difficulty,
            QuestionType questionType,
            List<String> supportedLanguages) {
        static QuestionVersionCatalogEntryResponse from(QuestionVersionCatalogEntry entry) {
            return new QuestionVersionCatalogEntryResponse(entry.questionId(), entry.questionVersionId(), entry.versionNumber(), entry.title(), entry.tags(), entry.difficulty(), entry.questionType(), entry.supportedLanguages());
        }
    }

    public record ResolveQuestionVersionsResponse(List<QuestionVersionCatalogEntryResponse> entries) {
        public static ResolveQuestionVersionsResponse from(List<QuestionVersionCatalogEntry> entries) {
            return new ResolveQuestionVersionsResponse(entries.stream().map(QuestionVersionCatalogEntryResponse::from).toList());
        }
    }
}
