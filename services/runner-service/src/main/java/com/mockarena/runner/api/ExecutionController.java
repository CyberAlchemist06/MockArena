package com.mockarena.runner.api;

import com.mockarena.runner.application.JavaSandboxExecutionService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/internal/v1/executions")
public class ExecutionController {
  private final JavaSandboxExecutionService executions;
  public ExecutionController(JavaSandboxExecutionService executions) { this.executions = executions; }
  @PostMapping public ResponseEntity<ExecutionResponse> execute(@Valid @RequestBody ExecutionRequest request) {
    return ResponseEntity.ok(executions.execute(request));
  }
}
