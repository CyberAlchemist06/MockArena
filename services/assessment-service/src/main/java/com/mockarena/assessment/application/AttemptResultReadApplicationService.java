package com.mockarena.assessment.application;

import com.mockarena.assessment.api.AssessmentDtos;
import com.mockarena.assessment.domain.*;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class AttemptResultReadApplicationService {
    private final AttemptRepository attempts;
    private final AssessmentVersionRepository versions;
    private final AttemptResultRepository results;
    private final AttemptItemResultRepository itemResults;
    private final Clock clock = Clock.systemUTC();

    public AttemptResultReadApplicationService(AttemptRepository attempts, AssessmentVersionRepository versions,
                                               AttemptResultRepository results, AttemptItemResultRepository itemResults) {
        this.attempts = attempts;
        this.versions = versions;
        this.results = results;
        this.itemResults = itemResults;
    }

    public AssessmentDtos.CandidateAttemptResultResponse result(UUID attemptId, UUID candidateUserId) {
        Attempt attempt = attempts.findById(attemptId).orElseThrow(AttemptNotFoundException::new);
        if (!attempt.candidateUserId().equals(candidateUserId)) throw new SecurityException("Attempt is not owned by current user");
        if (attempt.status() != AttemptStatus.SUBMITTED) throw new IllegalStateException("Attempt is not submitted");

        AttemptResult result = results.findById(attemptId).orElse(null);
        if (result == null) {
            return new AssessmentDtos.CandidateAttemptResultResponse(attemptId, EvaluationStatus.PENDING.name(), null, null, null, null, null, List.of());
        }
        AssessmentVersion version = versions.findById(attempt.assessmentVersionId()).orElseThrow(AssessmentVersionNotFoundException::new);
        enforceReleased(version);

        List<AssessmentDtos.AttemptResultItemResponse> items = itemResults.findByAttemptIdOrderByGlobalPositionAsc(attemptId).stream()
            .map(item -> new AssessmentDtos.AttemptResultItemResponse(item.globalPosition(), item.questionTypeCode(), item.evaluationStatus().name(),
                item.outcome() == null ? null : item.outcome().name(), item.awardedScore(), item.maxScore()))
            .toList();
        Instant releasedAt = effectiveReleasedAt(version, result);
        return new AssessmentDtos.CandidateAttemptResultResponse(attemptId, result.evaluationStatus().name(), result.rawScore(), result.maxScore(),
            result.percentage(), result.evaluatedAt(), releasedAt, items);
    }

    private void enforceReleased(AssessmentVersion version) {
        if ("IMMEDIATE".equals(version.resultReleasePolicyCode())) return;
        if ("SCHEDULED".equals(version.resultReleasePolicyCode())) {
            Instant releaseAt = Instant.parse(version.resultReleasePolicyParameters().path("releaseAt").asText());
            if (!clock.instant().isBefore(releaseAt)) return;
        }
        throw new ResultNotReleasedException();
    }

    private static Instant effectiveReleasedAt(AssessmentVersion version, AttemptResult result) {
        if ("IMMEDIATE".equals(version.resultReleasePolicyCode()) && result.evaluationStatus() == EvaluationStatus.EVALUATED) return result.evaluatedAt();
        if ("SCHEDULED".equals(version.resultReleasePolicyCode())) return Instant.parse(version.resultReleasePolicyParameters().path("releaseAt").asText());
        return null;
    }
}
