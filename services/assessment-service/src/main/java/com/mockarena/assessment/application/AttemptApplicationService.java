package com.mockarena.assessment.application;

import com.mockarena.assessment.domain.*;
import com.mockarena.assessment.infrastructure.challenge.*;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import java.nio.charset.StandardCharsets; import java.security.MessageDigest; import java.time.*; import java.util.*;

@Service
public class AttemptApplicationService {
    private final AssessmentRepository assessments; private final AssessmentVersionRepository versions; private final AssessmentVersionChallengeRepository assessmentManifest; private final AttemptRepository attempts; private final ChallengeVersionCatalogClient challenges; private final AttemptPersistenceService persistence; private final AttemptStartEntitlementPort entitlements; private final Clock clock = Clock.systemUTC();
    public AttemptApplicationService(AssessmentRepository assessments, AssessmentVersionRepository versions, AssessmentVersionChallengeRepository assessmentManifest, AttemptRepository attempts, ChallengeVersionCatalogClient challenges, AttemptPersistenceService persistence, AttemptStartEntitlementPort entitlements) { this.assessments=assessments; this.versions=versions; this.assessmentManifest=assessmentManifest; this.attempts=attempts; this.challenges=challenges; this.persistence=persistence; this.entitlements=entitlements; }
    public Attempt start(UUID assessmentId, int versionNumber, UUID candidateId, String idempotencyKey) {
        String key = validateKey(idempotencyKey); String fingerprint = fingerprint(assessmentId, versionNumber); Instant now = clock.instant();
        Attempt replay = persistence.replay(candidateId, key, fingerprint, now); if (replay != null) return reconcile(replay);
        Assessment assessment = assessments.findById(assessmentId).orElseThrow(AssessmentNotFoundException::new);
        AssessmentVersion version = versions.findByAssessmentIdAndVersionNumber(assessmentId, versionNumber).orElseThrow(AssessmentVersionNotFoundException::new);
        if (assessment.lifecycleStatus() != AssessmentStatus.PUBLISHED || !version.canStartAttemptAt(now)) throw new IllegalStateException("Assessment version is not available for attempts");
        Attempt active = persistence.expireAndFindActive(candidateId, version.id(), now); if (active != null) return persistence.recordResume(candidateId, assessmentId, version.id(), key, fingerprint, active, now);
        int maximum = version.attemptPolicyParameters().path("maxAttempts").asInt(0); if (!"MAX_ATTEMPTS".equals(version.attemptPolicyCode()) || maximum < 1 || attemptsUsed(candidateId, version.id()) >= maximum) throw new AttemptStartEntitlementDeniedException();
        List<AttemptItem> route = route(version);
        UUID reservation = entitlements.reserve(candidateId, "assessment.attempt.start", key);
        Attempt created;
        try {
            created = persistence.create(candidateId, assessmentId, version, key, fingerprint, reservation, now, route);
            if (!reservation.equals(created.entitlementReservationId())) { entitlements.release(reservation); return persistence.recordResume(candidateId, assessmentId, version.id(), key, fingerprint, created, now); }
        } catch (DataIntegrityViolationException exception) {
            entitlements.release(reservation); Attempt winner = persistence.expireAndFindActive(candidateId, version.id(), clock.instant()); if (winner != null) return persistence.recordResume(candidateId, assessmentId, version.id(), key, fingerprint, winner, clock.instant()); throw exception;
        } catch (RuntimeException exception) { entitlements.release(reservation); throw exception; }
        try { entitlements.commit(reservation); persistence.markEntitlementCommitted(reservation, clock.instant()); return created; }
        catch (RuntimeException exception) { throw new AttemptStartUnavailableException(exception); }
    }
    private long attemptsUsed(UUID candidate, UUID version) { return attempts.countByCandidateUserIdAndAssessmentVersionId(candidate, version); }
    private List<AttemptItem> route(AssessmentVersion version) {
        List<AssessmentVersionChallenge> stored = assessmentManifest.findByAssessmentVersionIdOrderByPositionAsc(version.id());
        List<ChallengeVersionManifest> resolved = challenges.resolveManifests(stored.stream().map(AssessmentVersionChallenge::challengeVersionId).toList());
        if (resolved.size() != stored.size()) throw new ChallengeVersionNotComposableException(); List<AttemptItem> route = new ArrayList<>(); int global = 1;
        for (int index = 0; index < stored.size(); index++) { AssessmentVersionChallenge expected = stored.get(index); ChallengeVersionManifest actual = resolved.get(index);
            if (!expected.challengeId().equals(actual.challengeId()) || !expected.challengeVersionId().equals(actual.challengeVersionId()) || actual.questions() == null || actual.questions().isEmpty()) throw new ChallengeVersionNotComposableException();
            for (ChallengeQuestionReference question : actual.questions()) { if (question.questionId() == null || question.questionVersionId() == null || question.questionTypeCode() == null || question.questionTypeCode().isBlank()) throw new ChallengeVersionNotComposableException(); route.add(new AttemptItem(null, global++, actual.challengeId(), actual.challengeVersionId(), expected.position(), question.questionId(), question.questionVersionId(), question.position(), question.questionTypeCode())); }
        } return route;
    }
    private Attempt reconcile(Attempt attempt) {
        UUID reservation = persistence.pendingReservationId(attempt.id()); if (reservation == null) return attempt;
        try { entitlements.commit(reservation); persistence.markEntitlementCommitted(reservation, clock.instant()); return attempt; }
        catch (RuntimeException exception) { throw new AttemptStartUnavailableException(exception); }
    }
    private static String validateKey(String key) { if (key == null || key.isBlank() || key.length() > 200) throw new IllegalArgumentException("Idempotency-Key is required"); return key.trim(); }
    private static String fingerprint(UUID assessmentId, int versionNumber) { try { return java.util.HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest((assessmentId + ":" + versionNumber).getBytes(StandardCharsets.UTF_8))); } catch (Exception exception) { throw new IllegalStateException(exception); } }
}
