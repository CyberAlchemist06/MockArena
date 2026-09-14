package com.mockarena.evaluation.application;
import com.fasterxml.jackson.databind.JsonNode; import java.util.*;
/** Transient trusted handoff only. No runner exists in Phase 2 and this object is never persisted or logged. */
public record RunnerJobPreparation(UUID jobId,String language,String runtimeProfileId,String sourceCode,JsonNode executionLimits,JsonNode hiddenTestSpecification,JsonNode comparison,JsonNode scoringPolicy){}
