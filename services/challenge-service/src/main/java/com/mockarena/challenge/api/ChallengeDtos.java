package com.mockarena.challenge.api;

import com.mockarena.challenge.domain.*;
import com.mockarena.challenge.question.QuestionCatalogEntry;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.List;
import java.util.UUID;

public final class ChallengeDtos {
    private ChallengeDtos() { }
    public record RuleBasedSelectionRequest(@Size(max = 20) List<@NotBlank @Size(max = 50) String> tagsAll,
                                            @Size(max = 3) List<@Pattern(regexp = "EASY|MEDIUM|HARD") String> difficulties,
                                            @Pattern(regexp = "^[A-Za-z][A-Za-z0-9_+.-]{0,31}$") String supportedLanguage,
                                            @NotNull @Min(1) @Max(100) Integer requestedQuestionCount) { }
    public record CreateChallengeRequest(@NotBlank @Size(max = 200) String title, @NotNull ChallengeVisibility visibility,
                                         @NotNull @Valid RuleBasedSelectionRequest selection) { }
    public record ResolvedQuestionResponse(int position, UUID questionId, UUID questionVersionId) { }
    public record ChallengeResponse(UUID challengeId, String lifecycleStatus, ChallengeVisibility visibility, UUID challengeVersionId,
                                    int versionNumber, String versionStatus, String title, RuleBasedSelectionRequest selection,
                                    List<ResolvedQuestionResponse> resolvedQuestions) {
        public static ChallengeResponse from(Challenge challenge, ChallengeVersion version, List<QuestionCatalogEntry> selected) {
            RuleBasedSelectionRequest rule = new RuleBasedSelectionRequest(version.tagsAll(), version.difficulties(), version.supportedLanguage(), version.requestedQuestionCount());
            List<ResolvedQuestionResponse> questions = java.util.stream.IntStream.range(0, selected.size()).mapToObj(index -> new ResolvedQuestionResponse(index + 1, selected.get(index).questionId(), selected.get(index).questionVersionId())).toList();
            return new ChallengeResponse(challenge.id(), challenge.lifecycleStatus().name(), challenge.visibility(), version.id(), version.versionNumber(), version.status().name(), version.title(), rule, questions);
        }
    }
}
