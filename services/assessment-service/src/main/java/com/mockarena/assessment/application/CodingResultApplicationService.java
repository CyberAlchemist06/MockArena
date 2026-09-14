package com.mockarena.assessment.application;

import com.mockarena.assessment.api.InternalCodingResultController.Request;
import com.mockarena.assessment.api.InternalCodingResultController.Response;
import com.mockarena.assessment.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
public class CodingResultApplicationService {
    private final AttemptRepository attempts;
    private final AttemptItemRepository items;
    private final SubmittedCodingResponseSnapshotRepository snapshots;
    private final AttemptItemResultRepository results;
    private final AttemptResultRepository aggregates;
    private final CodingResultApplicationReceiptRepository receipts;
    private final Clock clock = Clock.systemUTC();

    public CodingResultApplicationService(
            AttemptRepository attempts,
            AttemptItemRepository items,
            SubmittedCodingResponseSnapshotRepository snapshots,
            AttemptItemResultRepository results,
            AttemptResultRepository aggregates,
            CodingResultApplicationReceiptRepository receipts) {
        this.attempts = attempts;
        this.items = items;
        this.snapshots = snapshots;
        this.results = results;
        this.aggregates = aggregates;
        this.receipts = receipts;
    }

    @Transactional
    public Response apply(UUID attemptId, Request request) {
        Instant now = clock.instant();
        CodingResultApplicationReceipt prior = receipts.findById(request.evaluationJobId()).orElse(null);
        if (prior != null) {
            assertSameJobReplay(attemptId, request, prior);
            return new Response(attemptId,
                    aggregates.findById(attemptId).orElseThrow().evaluationStatus().name());
        }

        Attempt attempt = attempts.findByIdForUpdate(attemptId).orElseThrow(AttemptNotFoundException::new);
        if (attempt.status() != AttemptStatus.SUBMITTED) {
            throw new IllegalStateException("Attempt is not submitted");
        }

        AttemptItem item = items.findByAttemptIdAndGlobalPosition(attemptId, request.globalPosition()).orElseThrow();
        if (!"CODING".equals(item.questionTypeCode())
                || !item.questionId().equals(request.questionId())
                || !item.questionVersionId().equals(request.questionVersionId())) {
            throw new IllegalStateException("Coding identity mismatch");
        }

        SubmittedCodingResponseSnapshot snapshot = snapshots.findById(
                        new SubmittedCodingResponseSnapshotId(attemptId, request.globalPosition()))
                .orElseThrow();
        if (!Objects.equals(snapshot.sourceFingerprint(), request.sourceFingerprint())) {
            throw new IllegalStateException("Coding fingerprint mismatch");
        }

        EvaluationOutcome outcome = codingOutcome(request.outcome());
        validateScore(request.awardedScore(), request.maxScore());

        AttemptItemResponseId itemResultId = new AttemptItemResponseId(attemptId, request.globalPosition());
        AttemptItemResult existingItemResult = results.findById(itemResultId).orElse(null);
        if (existingItemResult != null && existingItemResult.evaluationStatus() == ItemEvaluationStatus.EVALUATED) {
            throw new IllegalStateException("Terminal coding result already applied");
        }
        if (existingItemResult == null) {
            existingItemResult = new AttemptItemResult(item, ItemEvaluationStatus.EVALUATED, outcome,
                    request.awardedScore(), request.maxScore(), now);
        } else {
            existingItemResult.replace(ItemEvaluationStatus.EVALUATED, outcome,
                    request.awardedScore(), request.maxScore(), now);
        }
        results.save(existingItemResult);
        AttemptResult aggregate = recomputeAggregate(attemptId, now);
        receipts.save(new CodingResultApplicationReceipt(request.evaluationJobId(), attemptId,
                request.globalPosition(), request.questionVersionId(), request.sourceFingerprint(), now));
        return new Response(attemptId, aggregate.evaluationStatus().name());
    }

    private void assertSameJobReplay(UUID attemptId, Request request, CodingResultApplicationReceipt prior) {
        if (!prior.attemptId().equals(attemptId)
                || prior.globalPosition() != request.globalPosition()
                || !prior.questionVersionId().equals(request.questionVersionId())
                || !Objects.equals(prior.sourceFingerprint(), request.sourceFingerprint())) {
            throw new IllegalStateException("Conflicting coding result");
        }
        AttemptItemResult persisted = results.findById(new AttemptItemResponseId(attemptId, request.globalPosition()))
                .orElseThrow(() -> new IllegalStateException("Coding receipt has no item result"));
        if (persisted.evaluationStatus() != ItemEvaluationStatus.EVALUATED
                || !"CODING".equals(persisted.questionTypeCode())
                || !persisted.questionId().equals(request.questionId())
                || !persisted.questionVersionId().equals(request.questionVersionId())
                || persisted.outcome() != codingOutcome(request.outcome())
                || persisted.awardedScore().compareTo(request.awardedScore()) != 0
                || persisted.maxScore().compareTo(request.maxScore()) != 0) {
            throw new IllegalStateException("Conflicting coding result");
        }
    }

    private AttemptResult recomputeAggregate(UUID attemptId, Instant now) {
        List<AttemptItemResult> allItemResults = results.findByAttemptIdOrderByGlobalPositionAsc(attemptId);
        int itemCount = items.findByAttemptIdOrderByGlobalPositionAsc(attemptId).size();
        boolean finalState = allItemResults.size() == itemCount
                && allItemResults.stream().allMatch(result -> result.evaluationStatus() == ItemEvaluationStatus.EVALUATED);
        AttemptResult aggregate = aggregates.findById(attemptId).orElseGet(() -> new AttemptResult(
                attemptId, EvaluationStatus.PARTIALLY_EVALUATED, null, null, null, now, null));

        if (!finalState) {
            aggregate.replace(EvaluationStatus.PARTIALLY_EVALUATED, null, null, null, null, null, now);
            return aggregates.save(aggregate);
        }

        BigDecimal score = allItemResults.stream().map(AttemptItemResult::awardedScore)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal maxScore = allItemResults.stream().map(AttemptItemResult::maxScore)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (maxScore.signum() <= 0) {
            throw new IllegalStateException("Final result has no positive maximum score");
        }
        BigDecimal percentage = score.multiply(BigDecimal.valueOf(100))
                .divide(maxScore, 4, RoundingMode.HALF_UP);
        aggregate.replace(EvaluationStatus.EVALUATED, score, maxScore, percentage, now, now, now);
        return aggregates.save(aggregate);
    }

    private EvaluationOutcome codingOutcome(String value) {
        EvaluationOutcome outcome = EvaluationOutcome.valueOf(value);
        return switch (outcome) {
            case PASSED, WRONG_ANSWER, COMPILE_ERROR, RUNTIME_ERROR,
                    TIME_LIMIT_EXCEEDED, OUTPUT_LIMIT_EXCEEDED, UNANSWERED -> outcome;
            default -> throw new IllegalArgumentException("Unsupported coding outcome");
        };
    }

    private void validateScore(BigDecimal awardedScore, BigDecimal maxScore) {
        if (awardedScore.signum() < 0 || maxScore.signum() <= 0
                || awardedScore.compareTo(maxScore) > 0) {
            throw new IllegalArgumentException("Invalid coding score");
        }
    }
}
