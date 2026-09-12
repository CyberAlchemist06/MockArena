package com.mockarena.assessment.infrastructure.challenge;

import java.util.List;
import java.util.UUID;

/** Safe, ID-only route owned and supplied by Challenge Service. */
public record ChallengeVersionManifest(
        UUID challengeId,
        UUID challengeVersionId,
        int challengeVersionNumber,
        List<ChallengeQuestionReference> questions) { }
