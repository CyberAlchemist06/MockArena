package com.mockarena.assessment.application;

import com.mockarena.assessment.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant; import java.util.*;

@Service
public class AttemptPersistenceService {
    private final AttemptRepository attempts; private final AttemptItemRepository items; private final AttemptStartRequestRepository requests; private final AttemptEntitlementReconciliationRepository reconciliations;
    public AttemptPersistenceService(AttemptRepository attempts, AttemptItemRepository items, AttemptStartRequestRepository requests, AttemptEntitlementReconciliationRepository reconciliations) { this.attempts=attempts; this.items=items; this.requests=requests; this.reconciliations=reconciliations; }
    @Transactional public Attempt expireAndFindActive(UUID candidateId, UUID assessmentVersionId, Instant now) {
        attempts.findByCandidateUserIdAndAssessmentVersionIdAndStatus(candidateId, assessmentVersionId, AttemptStatus.IN_PROGRESS).ifPresent(attempt -> { if (attempt.expireIfDue(now)) attempts.saveAndFlush(attempt); });
        return attempts.findByCandidateUserIdAndAssessmentVersionIdAndStatus(candidateId, assessmentVersionId, AttemptStatus.IN_PROGRESS).orElse(null);
    }
    @Transactional public Attempt replay(UUID candidateId, String key, String fingerprint, Instant now) {
        AttemptStartRequest request = requests.findByCandidateUserIdAndIdempotencyKey(candidateId, key).orElse(null); if (request == null) return null;
        if (!request.requestFingerprint().equals(fingerprint)) throw new IdempotencyKeyReusedException();
        Attempt attempt = attempts.findById(request.attemptId()).orElseThrow(); attempt.expireIfDue(now); return attempts.saveAndFlush(attempt);
    }
    @Transactional public Attempt create(UUID candidateId, UUID assessmentId, AssessmentVersion version, String key, String fingerprint, UUID reservationId, Instant now, List<AttemptItem> route) {
        Attempt existing = expireAndFindActive(candidateId, version.id(), now); if (existing != null) return existing;
        Attempt attempt = attempts.saveAndFlush(new Attempt(UUID.randomUUID(), assessmentId, version.id(), version.versionNumber(), candidateId, now, version.effectiveAttemptDeadline(now), reservationId));
        List<AttemptItem> persisted = route.stream().map(item -> new AttemptItem(attempt.id(), item.globalPosition(), item.challengeId(), item.challengeVersionId(), item.challengePosition(), item.questionId(), item.questionVersionId(), item.questionPosition(), item.questionTypeCode())).toList();
        items.saveAll(persisted);
        requests.save(new AttemptStartRequest(UUID.randomUUID(), candidateId, key, fingerprint, assessmentId, version.id(), attempt.id(), "CREATED", now));
        reconciliations.save(new AttemptEntitlementReconciliation(UUID.randomUUID(), attempt.id(), reservationId, "assessment.attempt.start", now));
        return attempt;
    }
    @Transactional public Attempt recordResume(UUID candidateId, UUID assessmentId, UUID assessmentVersionId, String key, String fingerprint, Attempt active, Instant now) {
        requests.findByCandidateUserIdAndIdempotencyKey(candidateId, key).ifPresentOrElse(existing -> { if (!existing.requestFingerprint().equals(fingerprint)) throw new IdempotencyKeyReusedException(); }, () -> requests.save(new AttemptStartRequest(UUID.randomUUID(), candidateId, key, fingerprint, assessmentId, assessmentVersionId, active.id(), "RESUMED", now)));
        return active;
    }
    @Transactional public void markEntitlementCommitted(UUID reservationId, Instant now) { reconciliations.findByReservationId(reservationId).ifPresent(value -> { value.committed(now); reconciliations.save(value); }); }
    @Transactional(readOnly = true) public UUID pendingReservationId(UUID attemptId) { return reconciliations.findByAttemptId(attemptId).filter(value -> "PENDING_COMMIT".equals(value.state())).map(AttemptEntitlementReconciliation::reservationId).orElse(null); }
}
