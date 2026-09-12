package com.mockarena.challenge.api;

import com.mockarena.challenge.domain.*;
import com.mockarena.challenge.question.QuestionCatalogEntry;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.List;
import java.util.UUID;

public final class ChallengeDtos {
    private ChallengeDtos() { }
    public record TaxonomyAssignmentRequest(@NotBlank @Size(max = 64) String scheme, @NotBlank @Size(max = 128) String code) { }
    public record DifficultyProfileRequest(@NotBlank @Size(max = 64) String scheme, @NotBlank @Size(max = 64) String code) { }
    public record RuleBasedSelectionRequest(@Valid @Size(max = 20) List<TaxonomyAssignmentRequest> taxonomyAll,
                                            @Size(max = 20) List<@NotBlank @Size(max = 64) String> questionTypeCodes,
                                            @Valid @Size(max = 20) List<DifficultyProfileRequest> difficultyProfiles,
                                            @Size(max = 20) List<@NotBlank @Size(max = 35) String> contentLocales,
                                            @Size(max = 20) List<@NotBlank @Size(max = 32) String> programmingLanguages,
                                            @NotNull @Min(1) @Max(100) Integer requestedQuestionCount) { }
    public record CreateChallengeRequest(@NotBlank @Size(max = 200) String title, @NotNull ChallengeVisibility visibility,
                                         @NotNull @Valid RuleBasedSelectionRequest selection) { }
    public record PublishChallengeVersionRequest(@Min(0) long expectedChallengeVersion, @Min(0) long expectedVersion) { }
    public record RetireChallengeVersionRequest(@Min(0) long expectedChallengeVersion, @Min(0) long expectedVersion) { }
    public record ResolvedQuestionResponse(int position, UUID questionId, UUID questionVersionId) { }
    public record ChallengeResponse(UUID challengeId, String lifecycleStatus, ChallengeVisibility visibility, UUID challengeVersionId,
                                    int versionNumber, String versionStatus, String title, RuleBasedSelectionRequest selection,
                                    List<ResolvedQuestionResponse> resolvedQuestions) {
        public static ChallengeResponse from(Challenge challenge, ChallengeVersion version, List<QuestionCatalogEntry> selected) {
            RuleBasedSelectionRequest rule = new RuleBasedSelectionRequest(
                    version.taxonomyAll().stream().map(value -> new TaxonomyAssignmentRequest(value.scheme(), value.code())).toList(),
                    version.questionTypeCodes(), version.difficultyProfiles().stream().map(value -> new DifficultyProfileRequest(value.scheme(), value.code())).toList(),
                    version.contentLocales(), version.programmingLanguages(), version.requestedQuestionCount());
            List<ResolvedQuestionResponse> questions = java.util.stream.IntStream.range(0, selected.size()).mapToObj(index -> new ResolvedQuestionResponse(index + 1, selected.get(index).questionId(), selected.get(index).questionVersionId())).toList();
            return new ChallengeResponse(challenge.id(), challenge.lifecycleStatus().name(), challenge.visibility(), version.id(), version.versionNumber(), version.status().name(), version.title(), rule, questions);
        }
    }
}
