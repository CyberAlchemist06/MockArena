package com.mockarena.assessment.infrastructure.challenge;

import java.util.UUID;

/** Contains routing metadata only; it deliberately has no Question content fields. */
public record ChallengeQuestionReference(
        int position,
        UUID questionId,
        UUID questionVersionId,
        String questionTypeCode) { }
