package com.mockarena.question.domain;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.UUID;

/** Protected immutable projection for the internal coding-evaluation use case only. */
public record CodingEvaluationData(
        UUID questionVersionId,
        String questionTypeCode,
        String runtimeProfileId,
        JsonNode allowedProgrammingLanguages,
        JsonNode executionLimits,
        JsonNode scoringPolicy,
        JsonNode testSpecification) { }
