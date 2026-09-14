package com.mockarena.runner.api;

import java.util.UUID;
public record ExecutionResponse(UUID jobId, String status, String candidateFailureCategory,
                                int testsPassed, int testsTotal, Long executionTimeMs, Long peakMemoryBytes) {}
