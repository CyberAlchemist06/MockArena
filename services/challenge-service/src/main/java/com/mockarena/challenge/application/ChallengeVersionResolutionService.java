package com.mockarena.challenge.application;

import com.mockarena.challenge.api.ChallengeVersionInternalDtos.Entry;
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
    private Entry resolveOne(UUID id) {
        ChallengeVersion version = versions.findById(id).orElseThrow(() -> new java.util.NoSuchElementException("Challenge version not found"));
        if (version.status() != ChallengeVersionStatus.PUBLISHED || composition.countByChallengeVersionId(id) != version.requestedQuestionCount()) throw new UncomposableChallengeVersionException();
        return new Entry(version.challengeId(), version.id(), version.versionNumber(), version.status().name());
    }
}
