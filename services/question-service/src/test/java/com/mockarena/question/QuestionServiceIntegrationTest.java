package com.mockarena.question;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class QuestionServiceIntegrationTest {
    @Container static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");
    @DynamicPropertySource static void database(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url", postgres::getJdbcUrl); r.add("spring.datasource.username", postgres::getUsername); r.add("spring.datasource.password", postgres::getPassword);
    }
    @Autowired MockMvc mvc; @Autowired ObjectMapper json; @Autowired JdbcTemplate jdbc;
    @Test void migrationsRun_and_candidateResponsesNeverContainHiddenTests() throws Exception {
        String body="""
          {"ownerUserId":"%s","content":{"title":"Two Sum","prompt":"Find pair","constraintsText":"n >= 2","examples":[],"supportedLanguages":["java"],"visibleTests":[{"input":"[2,7]","output":"[0,1]"}],"hiddenTests":[{"input":"secret-input","output":"secret-output"}],"scoringRules":{"points":100},"executionLimits":{"timeMs":1000}}}
          """.formatted(UUID.randomUUID());
        String response=mvc.perform(post("/api/v1/questions").contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isCreated()).andExpect(jsonPath("$.hiddenTests").doesNotExist()).andReturn().getResponse().getContentAsString();
        assertThatThrownBy(() -> json.readTree(response).required("hiddenTests")).isInstanceOf(IllegalArgumentException.class);
        String questionId=json.readTree(response).required("questionId").asText();
        mvc.perform(post("/api/v1/questions/{id}/versions/1/publish",questionId).contentType(MediaType.APPLICATION_JSON).content("{\"expectedQuestionVersion\":0,\"expectedVersion\":0}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.hiddenTests").doesNotExist());
        assertThatThrownBy(() -> jdbc.update("update question.question_versions set title = 'changed' where question_id = ? and version_number = 1", UUID.fromString(questionId)))
            .hasMessageContaining("published question versions are immutable");
    }
}
