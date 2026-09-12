package com.mockarena.challenge.application;

import com.mockarena.challenge.api.ChallengeVersionInternalDtos.Entry;
import com.mockarena.challenge.api.ChallengeVersionInternalDtos.ManifestEntry;
import com.mockarena.challenge.api.ChallengeVersionInternalDtos.ManifestQuestion;
import com.mockarena.challenge.domain.ChallengeVersion;
import com.mockarena.challenge.domain.ChallengeVersionRepository;
import com.mockarena.challenge.domain.ChallengeVersionStatus;
import com.mockarena.challenge.infrastructure.ChallengeCompositionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class ChallengeVersionResolutionService {
    private final ChallengeVersionRepository versions;
    private final ChallengeCompositionRepository composition;
    public ChallengeVersionResolutionService(ChallengeVersionRepository versions, ChallengeCompositionRepository composition) { this.versions = versions; this.composition = composition; }
    @Transactional(readOnly = true)
    public List<Entry> resolve(List<UUID> ids) {
        return ids.stream().map(this::resolveOne).toList();
    }
    @Transactional(readOnly = true)
    public List<ManifestEntry> resolveManifests(List<UUID> ids) {
        return ids.stream().map(this::resolveManifest).toList();
    }
    private Entry resolveOne(UUID id) {
        ChallengeVersion version = versions.findById(id).orElseThrow(() -> new java.util.NoSuchElementException("Challenge version not found"));
        if (version.status() != ChallengeVersionStatus.PUBLISHED || composition.countByChallengeVersionId(id) != version.requestedQuestionCount()) throw new UncomposableChallengeVersionException();
        return new Entry(version.challengeId(), version.id(), version.versionNumber(), version.status().name());
    }
    private ManifestEntry resolveManifest(UUID id) {
        ChallengeVersion version = requireComposable(id);
        List<ManifestQuestion> questions = composition.findByChallengeVersionId(id).stream()
                .map(entry -> new ManifestQuestion(0, entry.questionId(), entry.questionVersionId(), entry.questionTypeCode()))
                .toList();
        List<ManifestQuestion> ordered = java.util.stream.IntStream.range(0, questions.size())
                .mapToObj(index -> {
                    ManifestQuestion question = questions.get(index);
                    return new ManifestQuestion(index + 1, question.questionId(), question.questionVersionId(), question.questionTypeCode());
                }).toList();
        return new ManifestEntry(version.challengeId(), version.id(), version.versionNumber(), ordered);
    }
    private ChallengeVersion requireComposable(UUID id) {
        ChallengeVersion version = versions.findById(id).orElseThrow(() -> new java.util.NoSuchElementException("Challenge version not found"));
        if (version.status() != ChallengeVersionStatus.PUBLISHED || composition.countByChallengeVersionId(id) != version.requestedQuestionCount()) throw new UncomposableChallengeVersionException();
        return version;
    }
}
