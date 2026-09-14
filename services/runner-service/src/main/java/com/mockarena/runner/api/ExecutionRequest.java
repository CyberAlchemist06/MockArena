package com.mockarena.runner.api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.*;

public record ExecutionRequest(@NotNull UUID jobId, @NotBlank String language, @NotBlank String runtimeProfileId,
                               @NotNull String source, @Valid @NotNull Limits limits,
                               @NotEmpty List<@Valid TestCase> tests) {
  public record Limits(@Positive long compileTimeoutMs, @Positive long executionTimeoutMs,
                       @Positive int memoryMb, @Positive int maxOutputBytes, @Positive int maxProcesses) {}
  public record TestCase(@NotNull String input, @NotNull String expectedOutput, @NotBlank String comparisonMode) {}
}
