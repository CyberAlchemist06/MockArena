package com.mockarena.assessment.application;

import com.mockarena.assessment.api.AssessmentDtos.*;
import com.mockarena.assessment.domain.*;
import com.mockarena.assessment.infrastructure.challenge.*;
import jakarta.persistence.OptimisticLockException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.*;
import java.util.*;

@Service
public class AssessmentApplicationService {
    private final AssessmentRepository assessments; private final AssessmentVersionRepository versions; private final AssessmentVersionChallengeRepository manifests; private final ChallengeVersionCatalogClient challenges; private final PublicAssessmentCatalogueProjectionService catalogueProjections; private final Clock clock = Clock.systemUTC();
    public AssessmentApplicationService(AssessmentRepository assessments, AssessmentVersionRepository versions, AssessmentVersionChallengeRepository manifests, ChallengeVersionCatalogClient challenges, PublicAssessmentCatalogueProjectionService catalogueProjections) { this.assessments = assessments; this.versions = versions; this.manifests = manifests; this.challenges = challenges; this.catalogueProjections = catalogueProjections; }
    @Transactional
    public AssessmentVersionResponse create(CreateAssessmentRequest request, UUID createdByUserId) {
        List<ChallengeVersionReference> references = resolve(request.content().challengeVersionIds()); Instant now = clock.instant();
        Assessment assessment = assessments.save(new Assessment(UUID.randomUUID(), createdByUserId, request.visibility(), now));
        AssessmentVersion version = versions.saveAndFlush(new AssessmentVersion(UUID.randomUUID(), assessment.id(), 1, content(request.content()), now));
        replaceManifest(version.id(), references); return response(version);
    }
    @Transactional(readOnly = true) public AssessmentResponse assessment(UUID assessmentId) { return com.mockarena.assessment.api.AssessmentDtos.assessment(requireAssessment(assessmentId)); }
    @Transactional(readOnly = true) public List<AssessmentVersionResponse> versions(UUID assessmentId) { requireAssessment(assessmentId); return versions.findByAssessmentIdOrderByVersionNumberDesc(assessmentId).stream().map(this::response).toList(); }
    @Transactional(readOnly = true) public AssessmentVersionResponse version(UUID assessmentId, int versionNumber) { return response(requireVersion(assessmentId, versionNumber)); }
    @Transactional
    public AssessmentVersionResponse update(UUID assessmentId, int versionNumber, UpdateAssessmentVersionRequest request) {
        AssessmentVersion version = requireVersion(assessmentId, versionNumber); assertVersion(version.version(), request.expectedVersion());
        List<ChallengeVersionReference> references = resolve(request.content().challengeVersionIds()); version.update(content(request.content()), clock.instant()); versions.saveAndFlush(version); replaceManifest(version.id(), references); return response(version);
    }
    @Transactional
    public AssessmentVersionResponse revise(UUID assessmentId, CreateRevisionRequest request) {
        Assessment assessment = requireAssessment(assessmentId); assertVersion(assessment.version(), request.expectedAssessmentVersion());
        List<ChallengeVersionReference> references = resolve(request.content().challengeVersionIds()); AssessmentVersion source = versions.findTopByAssessmentIdOrderByVersionNumberDesc(assessmentId).orElseThrow(AssessmentVersionNotFoundException::new);
        Instant now = clock.instant(); AssessmentVersion version = versions.saveAndFlush(new AssessmentVersion(UUID.randomUUID(), assessmentId, source.versionNumber() + 1, content(request.content()), now));
        replaceManifest(version.id(), references); assessment.touch(now); assessments.saveAndFlush(assessment); return response(version);
    }
    @Transactional
    public AssessmentVersionResponse publish(UUID assessmentId, int versionNumber, PublishAssessmentVersionRequest request) {
        Assessment assessment = requireAssessment(assessmentId); AssessmentVersion version = requireVersion(assessmentId, versionNumber); assertVersion(assessment.version(), request.expectedAssessmentVersion()); assertVersion(version.version(), request.expectedVersion());
        if (version.status() != AssessmentVersionStatus.DRAFT) throw new IllegalStateException("Only draft assessment versions can be published");
        List<ChallengeVersionReference> references = resolve(manifests.findByAssessmentVersionIdOrderByPositionAsc(version.id()).stream().map(AssessmentVersionChallenge::challengeVersionId).toList());
        assertVersion(assessment.version(), request.expectedAssessmentVersion()); assertVersion(version.version(), request.expectedVersion()); if (version.status() != AssessmentVersionStatus.DRAFT) throw new IllegalStateException("Only draft assessment versions can be published");
        replaceManifest(version.id(), references); Instant now = clock.instant(); catalogueProjections.create(version, manifests.findByAssessmentVersionIdOrderByPositionAsc(version.id()), now); version.publish(now); assessment.publish(version.id(), now); versions.saveAndFlush(version); assessments.saveAndFlush(assessment); return response(version);
    }
    @Transactional
    public AssessmentVersionResponse retire(UUID assessmentId, int versionNumber, RetireAssessmentVersionRequest request) {
        Assessment assessment = requireAssessment(assessmentId); AssessmentVersion version = requireVersion(assessmentId, versionNumber); assertVersion(assessment.version(), request.expectedAssessmentVersion()); assertVersion(version.version(), request.expectedVersion());
        Instant now = clock.instant(); version.retire(now); assessment.clearCurrentPublishedVersion(version.id(), now); versions.saveAndFlush(version); assessments.saveAndFlush(assessment); return response(version);
    }
    @Transactional
    public AssessmentVersionResponse closeVersion(UUID assessmentId, int versionNumber, CloseAssessmentVersionRequest request) {
        Assessment assessment = requireAssessment(assessmentId); AssessmentVersion version = requireVersion(assessmentId, versionNumber); assertVersion(assessment.version(), request.expectedAssessmentVersion()); assertVersion(version.version(), request.expectedVersion());
        Instant now = clock.instant(); version.close(now); assessment.clearCurrentPublishedVersion(version.id(), now); versions.saveAndFlush(version); assessments.saveAndFlush(assessment); return response(version);
    }
    @Transactional
    public AssessmentResponse close(UUID assessmentId, CloseAssessmentRequest request) { Assessment assessment = requireAssessment(assessmentId); assertVersion(assessment.version(), request.expectedAssessmentVersion()); assessment.close(clock.instant()); return com.mockarena.assessment.api.AssessmentDtos.assessment(assessments.saveAndFlush(assessment)); }
    private List<ChallengeVersionReference> resolve(List<UUID> ids) { if (ids == null || ids.isEmpty() || new HashSet<>(ids).size() != ids.size()) throw new IllegalArgumentException("Challenge version manifest must be ordered and unique"); return challenges.resolve(ids); }
    private void replaceManifest(UUID assessmentVersionId, List<ChallengeVersionReference> references) { manifests.deleteByAssessmentVersionId(assessmentVersionId); List<AssessmentVersionChallenge> rows = new ArrayList<>(); for (int index = 0; index < references.size(); index++) { ChallengeVersionReference reference = references.get(index); rows.add(new AssessmentVersionChallenge(assessmentVersionId, index + 1, reference.challengeId(), reference.challengeVersionId(), reference.challengeVersionNumber())); } manifests.saveAll(rows); manifests.flush(); }
    private Assessment requireAssessment(UUID id) { return assessments.findById(id).orElseThrow(AssessmentNotFoundException::new); }
    private AssessmentVersion requireVersion(UUID assessmentId, int number) { return versions.findByAssessmentIdAndVersionNumber(assessmentId, number).orElseThrow(AssessmentVersionNotFoundException::new); }
    private AssessmentVersionResponse response(AssessmentVersion version) { return com.mockarena.assessment.api.AssessmentDtos.version(version, manifests.findByAssessmentVersionIdOrderByPositionAsc(version.id())); }
    private static void assertVersion(long actual, long expected) { if (actual != expected) throw new OptimisticLockException("Stale resource version"); }
    private static AssessmentVersion.Content content(ContentRequest value) { return new AssessmentVersion.Content(value.title(), value.description(), value.instructions(), value.assessmentTypeCode(), value.timingPolicy().policyCode(), value.timingPolicy().parameters(), value.availableFrom(), value.availableUntil(), value.attemptDurationSeconds(), value.attemptPolicy().policyCode(), value.attemptPolicy().parameters(), value.resultReleasePolicy().policyCode(), value.resultReleasePolicy().parameters()); }
}
