package com.mockarena.challenge.application;

import com.mockarena.challenge.api.ChallengeDtos.CreateChallengeRequest;
import com.mockarena.challenge.api.ChallengeDtos.ChallengeResponse;
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
import java.util.Locale;
import java.util.UUID;

@Service
public class ChallengeApplicationService {
    private final ChallengeRepository challenges; private final ChallengeVersionRepository versions; private final ChallengeCompositionRepository composition;
    private final QuestionCatalogClient catalog; private final DeterministicQuestionSelector selector; private final Clock clock = Clock.systemUTC();
    public ChallengeApplicationService(ChallengeRepository challenges, ChallengeVersionRepository versions, ChallengeCompositionRepository composition, QuestionCatalogClient catalog, DeterministicQuestionSelector selector) {
        this.challenges = challenges; this.versions = versions; this.composition = composition; this.catalog = catalog; this.selector = selector;
    }
    @Transactional
    public ChallengeResponse create(CreateChallengeRequest request) {
        RuleBasedSelection rule = normalize(request.selection());
        UUID seed = UUID.nameUUIDFromBytes((request.title() + "|" + rule.tagsAll() + "|" + rule.difficulties() + "|" + rule.supportedLanguage() + "|" + rule.requestedQuestionCount()).getBytes(StandardCharsets.UTF_8));
        List<QuestionCatalogEntry> selected = selector.select(catalog.resolve(rule), rule.requestedQuestionCount(), seed);
        Instant now = clock.instant(); Challenge challenge = challenges.save(new Challenge(UUID.randomUUID(), request.visibility(), now));
        ChallengeVersion version = versions.saveAndFlush(new ChallengeVersion(UUID.randomUUID(), challenge.id(), request.title(), rule, seed, now));
        composition.save(version.id(), selected);
        return ChallengeResponse.from(challenge, version, selected);
    }
    private static RuleBasedSelection normalize(com.mockarena.challenge.api.ChallengeDtos.RuleBasedSelectionRequest selection) {
        return new RuleBasedSelection(normalizeValues(selection.tagsAll(), false), normalizeValues(selection.difficulties(), true),
                selection.supportedLanguage() == null ? null : selection.supportedLanguage().trim().toUpperCase(Locale.ROOT), selection.requestedQuestionCount());
    }
    private static List<String> normalizeValues(List<String> values, boolean upperCase) {
        if (values == null) return List.of();
        return values.stream().map(value -> upperCase ? value.trim().toUpperCase(Locale.ROOT) : value.trim().toLowerCase(Locale.ROOT)).distinct().sorted().toList();
    }
}
