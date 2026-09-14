package com.mockarena.runner.application;

import com.mockarena.runner.api.*;
import com.mockarena.runner.infrastructure.DockerJavaSandbox;
import org.springframework.stereotype.Service;

@Service public class JavaSandboxExecutionService {
  private final DockerJavaSandbox sandbox;
  public JavaSandboxExecutionService(DockerJavaSandbox sandbox) { this.sandbox = sandbox; }
  public ExecutionResponse execute(ExecutionRequest request) {
    if (!"JAVA".equals(request.language()) || !"java-21-stdio-v1".equals(request.runtimeProfileId()))
      throw new IllegalArgumentException("Only JAVA / java-21-stdio-v1 is supported");
    if (request.tests().stream().anyMatch(t -> !"NORMALIZED_WHITESPACE".equals(t.comparisonMode())))
      throw new IllegalArgumentException("Unsupported comparison mode");
    return sandbox.execute(request);
  }
}
