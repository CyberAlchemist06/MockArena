package com.mockarena.assessment.application;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.mockarena.assessment.domain.*;
import com.mockarena.assessment.infrastructure.challenge.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.*;

@Service
public class PublicAssessmentCatalogueProjectionService {
    private final ChallengeVersionCatalogClient challenges;
    private final PublicAssessmentCatalogueProjectionRepository projections;

    public PublicAssessmentCatalogueProjectionService(ChallengeVersionCatalogClient challenges, PublicAssessmentCatalogueProjectionRepository projections) { this.challenges = challenges; this.projections = projections; }

    @Transactional
    public void create(AssessmentVersion version, List<AssessmentVersionChallenge> references, Instant publishedAt) {
        if (projections.existsById(version.id())) return;
        List<UUID> challengeVersionIds = references.stream().map(AssessmentVersionChallenge::challengeVersionId).toList();
        List<ChallengeVersionManifest> manifests = challenges.resolveManifests(challengeVersionIds);
        int questionCount = manifests.stream().mapToInt(manifest -> manifest.questions().size()).sum();
        if (questionCount < 1) throw new IllegalArgumentException("Published assessment must contain questions");
        TreeMap<String, Integer> counts = new TreeMap<>();
        manifests.forEach(manifest -> manifest.questions().forEach(question -> counts.merge(question.questionTypeCode(), 1, Integer::sum)));
        ObjectNode typeCounts = JsonNodeFactory.instance.objectNode(); counts.forEach(typeCounts::put);
        int maxAttempts = version.attemptPolicyParameters().path("maxAttempts").asInt(0);
        if (maxAttempts < 1) throw new IllegalArgumentException("Published assessment must have a valid attempt policy");
        Instant releaseAt = "SCHEDULED".equals(version.resultReleasePolicyCode()) ? Instant.parse(version.resultReleasePolicyParameters().path("releaseAt").asText()) : null;
        projections.saveAndFlush(new PublicAssessmentCatalogueProjection(version.id(), version.assessmentId(), version.versionNumber(), version.title(), version.description(), summary(version.instructions()), version.assessmentTypeCode(), version.timingPolicyCode(), version.attemptDurationSeconds(), version.availableFrom(), version.availableUntil(), maxAttempts, version.resultReleasePolicyCode(), releaseAt, questionCount, typeCounts, publishedAt));
    }

    private static String summary(String instructions) {
        if (instructions == null || instructions.isBlank()) return null;
        String normalized = instructions.trim().replaceAll("\\s+", " ");
        return normalized.length() <= 1000 ? normalized : normalized.substring(0, 1000);
    }
}
