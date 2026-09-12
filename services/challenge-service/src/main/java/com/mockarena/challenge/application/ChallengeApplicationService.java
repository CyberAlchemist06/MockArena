package com.mockarena.challenge.application;

import com.mockarena.challenge.api.ChallengeDtos.CreateChallengeRequest;
import com.mockarena.challenge.api.ChallengeDtos.ChallengeResponse;
import com.mockarena.challenge.api.ChallengeDtos.PublishChallengeVersionRequest;
import com.mockarena.challenge.api.ChallengeDtos.RetireChallengeVersionRequest;
import jakarta.persistence.OptimisticLockException;
import com.mockarena.challenge.domain.*;
import com.mockarena.challenge.infrastructure.ChallengeCompositionRepository;
import com.mockarena.challenge.question.QuestionCatalogClient;
import com.mockarena.challenge.question.QuestionCatalogEntry;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class ChallengeApplicationService {
    private final ChallengeRepository challenges; private final ChallengeVersionRepository versions; private final ChallengeCompositionRepository composition;
    private final QuestionCatalogClient catalog; private final DeterministicQuestionSelector selector; private final Clock clock = Clock.systemUTC();
    public ChallengeApplicationService(ChallengeRepository challenges, ChallengeVersionRepository versions, ChallengeCompositionRepository composition, QuestionCatalogClient catalog, DeterministicQuestionSelector selector) {
        this.challenges = challenges; this.versions = versions; this.composition = composition; this.catalog = catalog; this.selector = selector;
    }
    @Transactional
    public ChallengeResponse create(CreateChallengeRequest request, UUID createdByUserId) {
        RuleBasedSelection rule = normalize(request.selection());
        UUID seed = UUID.nameUUIDFromBytes((request.title() + "|" + rule.taxonomyAll() + "|" + rule.questionTypeCodes() + "|" + rule.difficultyProfiles() + "|" + rule.contentLocales() + "|" + rule.programmingLanguages() + "|" + rule.requestedQuestionCount()).getBytes(StandardCharsets.UTF_8));
        List<QuestionCatalogEntry> selected = selector.select(catalog.resolve(rule), rule.requestedQuestionCount(), seed);
        Instant now = clock.instant(); Challenge challenge = challenges.save(new Challenge(UUID.randomUUID(), createdByUserId, request.visibility(), now));
        ChallengeVersion version = versions.saveAndFlush(new ChallengeVersion(UUID.randomUUID(), challenge.id(), request.title(), rule, seed, now));
        composition.save(version.id(), selected);
        return ChallengeResponse.from(challenge, version, selected);
    }
    @Transactional
    public ChallengeResponse publish(UUID challengeId, int versionNumber, PublishChallengeVersionRequest request) {
        Challenge challenge = requireChallenge(challengeId);
        ChallengeVersion version = requireVersion(challengeId, versionNumber);
        assertVersion(challenge.version(), request.expectedChallengeVersion());
        assertVersion(version.version(), request.expectedVersion());
        if (version.status() != ChallengeVersionStatus.DRAFT) throw new IllegalStateException("Only draft challenge versions can be published");

        List<QuestionCatalogEntry> selected = selector.select(catalog.resolve(ruleOf(version)), version.requestedQuestionCount(), version.selectionSeed());
        // The catalogue call occurs before local mutation. Everything below is one local transaction.
        assertVersion(challenge.version(), request.expectedChallengeVersion());
        assertVersion(version.version(), request.expectedVersion());
        if (version.status() != ChallengeVersionStatus.DRAFT) throw new IllegalStateException("Only draft challenge versions can be published");
        Instant now = clock.instant();
        composition.replace(version.id(), selected);
        version.publish(now);
        challenge.publish(version.id(), now);
        versions.saveAndFlush(version);
        challenges.saveAndFlush(challenge);
        return ChallengeResponse.from(challenge, version, selected);
    }
    @Transactional
    public ChallengeResponse retire(UUID challengeId, int versionNumber, RetireChallengeVersionRequest request) {
        Challenge challenge = requireChallenge(challengeId);
        ChallengeVersion version = requireVersion(challengeId, versionNumber);
        assertVersion(challenge.version(), request.expectedChallengeVersion());
        assertVersion(version.version(), request.expectedVersion());
        List<QuestionCatalogEntry> manifest = composition.findByChallengeVersionId(version.id());
        Instant now = clock.instant();
        version.retire(now);
        challenge.clearCurrentPublishedVersion(version.id(), now);
        versions.saveAndFlush(version);
        challenges.saveAndFlush(challenge);
        return ChallengeResponse.from(challenge, version, manifest);
    }
    private Challenge requireChallenge(UUID id) { return challenges.findById(id).orElseThrow(() -> new java.util.NoSuchElementException("Challenge not found")); }
    private ChallengeVersion requireVersion(UUID challengeId, int number) {
        ChallengeVersion result = versions.findByChallengeIdAndVersionNumber(challengeId, number).orElseThrow(() -> new java.util.NoSuchElementException("Challenge version not found"));
        if (!result.challengeId().equals(challengeId)) throw new java.util.NoSuchElementException("Challenge version not found");
        return result;
    }
    private static RuleBasedSelection ruleOf(ChallengeVersion value) { return new RuleBasedSelection(value.taxonomyAll(), value.questionTypeCodes(), value.difficultyProfiles(), value.contentLocales(), value.programmingLanguages(), value.requestedQuestionCount()); }
    private static void assertVersion(long actual, long expected) { if (actual != expected) throw new OptimisticLockException("Stale resource version"); }
    private static RuleBasedSelection normalize(com.mockarena.challenge.api.ChallengeDtos.RuleBasedSelectionRequest selection) {
        return new RuleBasedSelection(normalizeTaxonomy(selection.taxonomyAll()), normalizeUpper(selection.questionTypeCodes()),
                normalizeProfiles(selection.difficultyProfiles()), normalizeValues(selection.contentLocales()), normalizeUpper(selection.programmingLanguages()), selection.requestedQuestionCount());
    }
    private static List<String> normalizeValues(List<String> values) {
        if (values == null) return List.of();
        return values.stream().map(value -> value.trim().toLowerCase(java.util.Locale.ROOT)).distinct().sorted().toList();
    }
    private static List<String> normalizeUpper(List<String> values) {
        if (values == null) return List.of();
        return values.stream().map(value -> value.trim().toUpperCase(java.util.Locale.ROOT)).distinct().sorted().toList();
    }
    private static List<TaxonomyAssignment> normalizeTaxonomy(List<com.mockarena.challenge.api.ChallengeDtos.TaxonomyAssignmentRequest> values) {
        if (values == null) return List.of();
        return values.stream().map(value -> new TaxonomyAssignment(value.scheme().trim().toLowerCase(java.util.Locale.ROOT), value.code().trim().toLowerCase(java.util.Locale.ROOT)))
                .distinct().sorted(java.util.Comparator.comparing(TaxonomyAssignment::scheme).thenComparing(TaxonomyAssignment::code)).toList();
    }
    private static List<DifficultyProfile> normalizeProfiles(List<com.mockarena.challenge.api.ChallengeDtos.DifficultyProfileRequest> values) {
        if (values == null) return List.of();
        return values.stream().map(value -> new DifficultyProfile(value.scheme().trim().toLowerCase(java.util.Locale.ROOT), value.code().trim().toUpperCase(java.util.Locale.ROOT)))
                .distinct().sorted(java.util.Comparator.comparing(DifficultyProfile::scheme).thenComparing(DifficultyProfile::code)).toList();
    }
}
