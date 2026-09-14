package com.mockarena.assessment.application;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.mockarena.assessment.domain.*;
import com.mockarena.assessment.infrastructure.question.McqEvaluationData;
import com.mockarena.assessment.infrastructure.question.QuestionEvaluationDataClient;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.SimpleTransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AttemptEvaluationServiceTest {
    @Test void evaluatesFrozenMcqVersionAndPersistsDerivedFactsOnly() {
        AttemptRepository attempts = mock(AttemptRepository.class);
        AttemptItemRepository items = mock(AttemptItemRepository.class);
        AttemptItemResponseRepository responses = mock(AttemptItemResponseRepository.class);
        AttemptResultRepository results = mock(AttemptResultRepository.class);
        AttemptItemResultRepository itemResults = mock(AttemptItemResultRepository.class);
        QuestionEvaluationDataClient questions = mock(QuestionEvaluationDataClient.class);
        PlatformTransactionManager manager = mock(PlatformTransactionManager.class);
        when(manager.getTransaction(any())).thenReturn(new SimpleTransactionStatus());
        TransactionTemplate tx = new TransactionTemplate(manager);
        AttemptEvaluationService service = new AttemptEvaluationService(attempts, items, responses, results, itemResults, questions, tx);

        UUID attemptId = UUID.randomUUID(), versionId = UUID.randomUUID();
        Attempt attempt = submitted(attemptId);
        AttemptItem item = new AttemptItem(attemptId, 1, UUID.randomUUID(), UUID.randomUUID(), 1, UUID.randomUUID(), versionId, 1, "MCQ");
        var payload = JsonNodeFactory.instance.objectNode().put("selectedOptionId", "right");
        AttemptItemResponse response = new AttemptItemResponse(attemptId, 1, "MCQ", payload, Instant.now());
        var policy = JsonNodeFactory.instance.objectNode().set("parameters", JsonNodeFactory.instance.objectNode().put("correctPoints", 2).put("incorrectPoints", -1).put("unansweredPoints", 0));

        when(attempts.findById(attemptId)).thenReturn(Optional.of(attempt));
        when(attempts.findByIdForUpdate(attemptId)).thenReturn(Optional.of(attempt));
        when(items.findByAttemptIdOrderByGlobalPositionAsc(attemptId)).thenReturn(List.of(item));
        when(responses.findByAttemptIdOrderByGlobalPositionAsc(attemptId)).thenReturn(List.of(response));
        when(results.findById(attemptId)).thenReturn(Optional.empty());
        when(questions.resolve(List.of(versionId))).thenReturn(List.of(new McqEvaluationData(versionId, "MCQ", "right", policy)));
        when(results.saveAndFlush(any(AttemptResult.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AttemptResult result = service.evaluate(attemptId);

        assertThat(result.evaluationStatus()).isEqualTo(EvaluationStatus.EVALUATED);
        assertThat(result.rawScore()).isEqualByComparingTo("2");
        assertThat(result.maxScore()).isEqualByComparingTo("2");
        assertThat(result.percentage()).isEqualByComparingTo("100.0000");
        verify(questions).resolve(List.of(versionId));
        verify(itemResults).saveAll(argThat(saved -> {
            List<AttemptItemResult> values = new ArrayList<>(); saved.forEach(values::add);
            return values.size() == 1 && values.getFirst().outcome() == EvaluationOutcome.CORRECT && values.getFirst().awardedScore().compareTo(BigDecimal.valueOf(2)) == 0;
        }));
    }

    @Test void mixedAttemptIsStablePartialAndDoesNotRequeryQuestionServiceOnRetry() {
        AttemptRepository attempts = mock(AttemptRepository.class);
        AttemptItemRepository items = mock(AttemptItemRepository.class);
        AttemptItemResponseRepository responses = mock(AttemptItemResponseRepository.class);
        AttemptResultRepository results = mock(AttemptResultRepository.class);
        AttemptItemResultRepository itemResults = mock(AttemptItemResultRepository.class);
        QuestionEvaluationDataClient questions = mock(QuestionEvaluationDataClient.class);
        PlatformTransactionManager manager = mock(PlatformTransactionManager.class);
        when(manager.getTransaction(any())).thenReturn(new SimpleTransactionStatus());
        TransactionTemplate tx = new TransactionTemplate(manager);
        AttemptEvaluationService service = new AttemptEvaluationService(attempts, items, responses, results, itemResults, questions, tx);

        UUID attemptId = UUID.randomUUID(), versionId = UUID.randomUUID();
        Attempt attempt = submitted(attemptId);
        AttemptResult partial = new AttemptResult(attemptId, EvaluationStatus.PARTIALLY_EVALUATED, null, null, null, null, null);
        when(attempts.findById(attemptId)).thenReturn(Optional.of(attempt));
        when(results.findById(attemptId)).thenReturn(Optional.of(partial));

        AttemptResult retried = service.evaluate(attemptId);

        assertThat(retried).isSameAs(partial);
        verifyNoInteractions(items, responses, itemResults, questions);
    }

    private static Attempt submitted(UUID attemptId) {
        Instant now = Instant.now();
        Attempt attempt = new Attempt(attemptId, UUID.randomUUID(), UUID.randomUUID(), 1, UUID.randomUUID(), now, now.plusSeconds(300), null);
        attempt.submit(now);
        return attempt;
    }
}
