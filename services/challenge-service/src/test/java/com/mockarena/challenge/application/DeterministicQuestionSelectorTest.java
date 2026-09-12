package com.mockarena.challenge.application;

import com.mockarena.challenge.question.QuestionCatalogEntry;
import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DeterministicQuestionSelectorTest {
    private final DeterministicQuestionSelector selector = new DeterministicQuestionSelector();
    @Test void selectsTheSameOrderedSubsetForTheSameSeedAndRemovesDuplicateQuestions() {
        UUID firstQuestion = UUID.randomUUID();
        List<QuestionCatalogEntry> candidates = List.of(new QuestionCatalogEntry(firstQuestion, UUID.randomUUID()), new QuestionCatalogEntry(firstQuestion, UUID.randomUUID()),
                new QuestionCatalogEntry(UUID.randomUUID(), UUID.randomUUID()), new QuestionCatalogEntry(UUID.randomUUID(), UUID.randomUUID()));
        UUID seed = UUID.randomUUID();
        assertThat(selector.select(candidates, 2, seed)).containsExactlyElementsOf(selector.select(candidates, 2, seed));
        assertThat(selector.select(candidates, 2, seed)).extracting(QuestionCatalogEntry::questionId).doesNotHaveDuplicates();
    }
    @Test void rejectsAnInsufficientCatalog() {
        assertThatThrownBy(() -> selector.select(List.of(new QuestionCatalogEntry(UUID.randomUUID(), UUID.randomUUID())), 2, UUID.randomUUID())).isInstanceOf(InsufficientQuestionsException.class);
    }
}
