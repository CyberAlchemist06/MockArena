package com.mockarena.question.api;

import com.mockarena.question.domain.TaxonomyAssignment;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.List;
import java.util.UUID;

public final class QuestionCatalogV2Dtos {
    private QuestionCatalogV2Dtos() { }
    public record TaxonomyFilter(@NotBlank @Size(max = 64) String scheme, @NotBlank @Size(max = 128) String code) { }
    public record DifficultyProfile(@NotBlank @Size(max = 64) String scheme, @NotBlank @Size(max = 64) String code) { }
    public record ResolveRequest(@Size(max = 20) List<@Valid TaxonomyFilter> taxonomyAll,
                                 @Size(max = 20) List<@NotBlank @Size(max = 64) String> questionTypeCodes,
                                 @Size(max = 20) List<@Valid DifficultyProfile> difficultyProfiles,
                                 @Size(max = 20) List<@NotBlank @Size(max = 35) String> contentLocales,
                                 @Size(max = 20) List<@NotBlank @Size(max = 32) String> programmingLanguages,
                                 @NotNull @Min(1) @Max(100) Integer limit) { }
    public record Entry(UUID questionId, UUID questionVersionId, int versionNumber, String title, String questionTypeCode,
                        String contentLocale, List<TaxonomyAssignment> taxonomy, DifficultyProfile difficultyProfile,
                        List<String> programmingLanguages) { }
    public record ResolveResponse(List<Entry> entries) { }
}
