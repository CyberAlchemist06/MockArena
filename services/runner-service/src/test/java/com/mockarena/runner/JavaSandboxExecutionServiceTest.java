package com.mockarena.runner;

import com.mockarena.runner.api.ExecutionRequest;
import com.mockarena.runner.application.*;
import com.mockarena.runner.infrastructure.DockerJavaSandbox;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.assertj.core.api.Assertions.*;

class JavaSandboxExecutionServiceTest {
 @Test void normalizedWhitespaceComparisonIsDeterministic(){assertThat(NormalizedWhitespaceComparator.matches("  hello\n world ","hello   world")).isTrue();assertThat(NormalizedWhitespaceComparator.matches("one","two")).isFalse();}
 @Test void rejectsUnsupportedLanguageBeforeAnyDockerExecution(){var service=new JavaSandboxExecutionService(new DockerJavaSandbox("docker","unused",false,512,1024,32,1));assertThatThrownBy(()->service.execute(request("PYTHON","java-21-stdio-v1","NORMALIZED_WHITESPACE"))).isInstanceOf(IllegalArgumentException.class);}
 @Test void rejectsUnsupportedProfileAndComparisonBeforeAnyDockerExecution(){var service=new JavaSandboxExecutionService(new DockerJavaSandbox("docker","unused",false,512,1024,32,1));assertThatThrownBy(()->service.execute(request("JAVA","other","NORMALIZED_WHITESPACE"))).isInstanceOf(IllegalArgumentException.class);assertThatThrownBy(()->service.execute(request("JAVA","java-21-stdio-v1","EXACT_TEXT"))).isInstanceOf(IllegalArgumentException.class);}
 private static ExecutionRequest request(String language,String profile,String mode){return new ExecutionRequest(UUID.randomUUID(),language,profile,"class Main {}",new ExecutionRequest.Limits(1000,1000,128,1024,8),List.of(new ExecutionRequest.TestCase("","",mode)));}
}
