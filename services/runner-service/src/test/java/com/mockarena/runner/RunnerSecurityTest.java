package com.mockarena.runner;

import org.junit.jupiter.api.Test; import org.springframework.beans.factory.annotation.Autowired; import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc; import org.springframework.boot.test.context.SpringBootTest; import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post; import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
@SpringBootTest(properties={"runner.security.workload-token=runner-test-token","runner.docker.enabled=false"}) @AutoConfigureMockMvc class RunnerSecurityTest {
 @Autowired MockMvc mvc;
 @Test void internalExecutionRequiresWorkloadIdentity() throws Exception {String body="{\"jobId\":\"00000000-0000-0000-0000-000000000001\",\"language\":\"JAVA\",\"runtimeProfileId\":\"java-21-stdio-v1\",\"source\":\"class Main{}\",\"limits\":{\"compileTimeoutMs\":1,\"executionTimeoutMs\":1,\"memoryMb\":1,\"maxOutputBytes\":1,\"maxProcesses\":1},\"tests\":[{\"input\":\"\",\"expectedOutput\":\"\",\"comparisonMode\":\"NORMALIZED_WHITESPACE\"}]}";mvc.perform(post("/internal/v1/executions").contentType("application/json").content(body)).andExpect(status().isUnauthorized());mvc.perform(post("/internal/v1/executions").header("X-MockArena-Workload-Token","runner-test-token").contentType("application/json").content(body)).andExpect(status().isOk());}
}
