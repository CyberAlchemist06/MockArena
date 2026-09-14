package com.mockarena.question.domain;
import com.fasterxml.jackson.databind.JsonNode; import java.util.UUID;
/** Protected immutable projection for the internal evaluation use case only. */
public record McqEvaluationData(UUID questionVersionId,String questionTypeCode,String correctOptionId,JsonNode scoringPolicy) { }
