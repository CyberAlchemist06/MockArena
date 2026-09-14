package com.mockarena.assessment.application;

import com.mockarena.assessment.api.InternalCodingResultController.Request;
import com.mockarena.assessment.domain.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class CodingResultApplicationServiceTest {
    private AttemptRepository attempts;
    private AttemptItemRepository items;
    private SubmittedCodingResponseSnapshotRepository snapshots;
    private AttemptItemResultRepository itemResults;
    private AttemptResultRepository aggregates;
    private CodingResultApplicationReceiptRepository receipts;
    private CodingResultApplicationService service;
    private UUID attemptId;
    private AttemptItem mcq;
    private AttemptItem codingA;
    private AttemptItem codingB;
    private final Map<AttemptItemResponseId, AttemptItemResult> persistedItems = new LinkedHashMap<>();
    private final Map<UUID, AttemptResult> persistedAggregates = new HashMap<>();
    private final Map<UUID, CodingResultApplicationReceipt> persistedReceipts = new HashMap<>();

    @BeforeEach
    void setUp() {
        attempts = mock(AttemptRepository.class);
        items = mock(AttemptItemRepository.class);
        snapshots = mock(SubmittedCodingResponseSnapshotRepository.class);
        itemResults = mock(AttemptItemResultRepository.class);
        aggregates = mock(AttemptResultRepository.class);
        receipts = mock(CodingResultApplicationReceiptRepository.class);
        service = new CodingResultApplicationService(attempts, items, snapshots, itemResults, aggregates, receipts);

        attemptId = UUID.randomUUID();
        Attempt attempt = new Attempt(attemptId, UUID.randomUUID(), UUID.randomUUID(), 1, UUID.randomUUID(), Instant.now(), null, null);
        attempt.submit(Instant.now());
        mcq = item(1, "MCQ");
        codingA = item(2, "CODING");
        codingB = item(3, "CODING");

        when(attempts.findByIdForUpdate(attemptId)).thenReturn(Optional.of(attempt));
        when(items.findByAttemptIdOrderByGlobalPositionAsc(attemptId)).thenAnswer(invocation -> List.of(mcq, codingA, codingB));
        when(items.findByAttemptIdAndGlobalPosition(eq(attemptId), anyInt())).thenAnswer(invocation -> {
            int position = invocation.getArgument(1);
            return Optional.of(List.of(mcq, codingA, codingB).get(position - 1));
        });
        SubmittedCodingResponseSnapshot snapshot = mock(SubmittedCodingResponseSnapshot.class);
        when(snapshot.sourceFingerprint()).thenReturn("fingerprint");
        when(snapshots.findById(any())).thenReturn(Optional.of(snapshot));
        when(itemResults.existsById(any())).thenAnswer(invocation -> persistedItems.containsKey(invocation.getArgument(0)));
        when(itemResults.findById(any())).thenAnswer(invocation -> Optional.ofNullable(persistedItems.get(invocation.getArgument(0))));
        when(itemResults.findByAttemptIdOrderByGlobalPositionAsc(attemptId)).thenAnswer(invocation -> persistedItems.values().stream()
                .sorted(Comparator.comparingInt(AttemptItemResult::globalPosition)).toList());
        when(itemResults.save(any())).thenAnswer(invocation -> {
            AttemptItemResult result = invocation.getArgument(0);
            persistedItems.put(new AttemptItemResponseId(result.attemptId(), result.globalPosition()), result);
            return result;
        });
        when(aggregates.findById(attemptId)).thenAnswer(invocation -> Optional.ofNullable(persistedAggregates.get(attemptId)));
        when(aggregates.save(any())).thenAnswer(invocation -> {
            AttemptResult aggregate = invocation.getArgument(0);
            persistedAggregates.put(aggregate.attemptId(), aggregate);
            return aggregate;
        });
        when(receipts.findById(any())).thenAnswer(invocation -> Optional.ofNullable(persistedReceipts.get(invocation.getArgument(0))));
        when(receipts.save(any())).thenAnswer(invocation -> {
            CodingResultApplicationReceipt receipt = invocation.getArgument(0);
            persistedReceipts.put(receipt.evaluationJobId(), receipt);
            return receipt;
        });

        persistedItems.put(new AttemptItemResponseId(attemptId, 1), new AttemptItemResult(mcq,
                ItemEvaluationStatus.EVALUATED, EvaluationOutcome.CORRECT, BigDecimal.ONE, BigDecimal.ONE, Instant.now()));
    }

    @Test
    void appliesPassedCodingResultAndPreservesMcqInFinalAggregate() {
        apply(codingA, "PASSED", new BigDecimal("37"), new BigDecimal("37"));
        apply(codingB, "WRONG_ANSWER", BigDecimal.ZERO, new BigDecimal("62"));

        AttemptResult aggregate = persistedAggregates.get(attemptId);
        assertThat(aggregate.evaluationStatus()).isEqualTo(EvaluationStatus.EVALUATED);
        assertThat(aggregate.rawScore()).isEqualByComparingTo("38");
        assertThat(aggregate.maxScore()).isEqualByComparingTo("100");
        assertThat(aggregate.percentage()).isEqualByComparingTo("38.0000");
        assertThat(persistedItems.get(new AttemptItemResponseId(attemptId, 1)).outcome()).isEqualTo(EvaluationOutcome.CORRECT);
    }

    @Test
    void failingAndUnansweredCodingResultsReceiveZeroAndRemainPartialUntilAllItemsExist() {
        persistedItems.put(new AttemptItemResponseId(attemptId, 2), new AttemptItemResult(codingA,
                ItemEvaluationStatus.PENDING, null, null, null, null));
        apply(codingA, "COMPILE_ERROR", BigDecimal.ZERO, new BigDecimal("37"));
        assertThat(persistedAggregates.get(attemptId).evaluationStatus()).isEqualTo(EvaluationStatus.PARTIALLY_EVALUATED);
        assertThat(persistedItems.get(new AttemptItemResponseId(attemptId, 2)).evaluationStatus())
                .isEqualTo(ItemEvaluationStatus.EVALUATED);
        apply(codingB, "UNANSWERED", BigDecimal.ZERO, new BigDecimal("62"));

        AttemptResult aggregate = persistedAggregates.get(attemptId);
        assertThat(aggregate.evaluationStatus()).isEqualTo(EvaluationStatus.EVALUATED);
        assertThat(aggregate.rawScore()).isEqualByComparingTo("1");
        assertThat(aggregate.maxScore()).isEqualByComparingTo("100");
    }

    @Test
    void exactSameJobReplayIsSafeButConflictAndDifferentJobOverwriteAreRejected() {
        UUID jobId = UUID.randomUUID();
        Request first = request(jobId, codingA, "PASSED", new BigDecimal("37"), new BigDecimal("37"));
        service.apply(attemptId, first);
        service.apply(attemptId, first);

        assertThat(persistedReceipts).hasSize(1);
        assertThat(persistedItems).hasSize(2);
        assertThatThrownBy(() -> service.apply(attemptId,
                request(jobId, codingA, "WRONG_ANSWER", BigDecimal.ZERO, new BigDecimal("37"))))
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> service.apply(attemptId,
                request(UUID.randomUUID(), codingA, "PASSED", new BigDecimal("37"), new BigDecimal("37"))))
                .isInstanceOf(IllegalStateException.class);
    }

    private void apply(AttemptItem item, String outcome, BigDecimal awarded, BigDecimal max) {
        service.apply(attemptId, request(UUID.randomUUID(), item, outcome, awarded, max));
    }

    private Request request(UUID jobId, AttemptItem item, String outcome, BigDecimal awarded, BigDecimal max) {
        return new Request(jobId, item.globalPosition(), item.questionId(), item.questionVersionId(),
                "fingerprint", outcome, awarded, max);
    }

    private AttemptItem item(int position, String type) {
        return new AttemptItem(attemptId, position, UUID.randomUUID(), UUID.randomUUID(), 1,
                UUID.randomUUID(), UUID.randomUUID(), position, type);
    }
}
