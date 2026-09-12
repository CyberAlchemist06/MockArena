package com.mockarena.challenge;

import com.mockarena.challenge.domain.RuleBasedSelection;
import com.mockarena.challenge.question.QuestionCatalogClient;
import com.mockarena.challenge.question.QuestionCatalogEntry;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import java.util.List;
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class ChallengeServiceIntegrationTest {
    @Container static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");
    @DynamicPropertySource static void database(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl); registry.add("spring.datasource.username", postgres::getUsername); registry.add("spring.datasource.password", postgres::getPassword);
    }
    @Autowired MockMvc mvc; @Autowired JdbcTemplate jdbc;
    @MockitoBean QuestionCatalogClient catalog;

    @Test void createsDraftChallengeAndStoresOnlyResolvedQuestionReferences() throws Exception {
        QuestionCatalogEntry first = new QuestionCatalogEntry(UUID.randomUUID(), UUID.randomUUID());
        QuestionCatalogEntry second = new QuestionCatalogEntry(UUID.randomUUID(), UUID.randomUUID());
        when(catalog.resolve(any(RuleBasedSelection.class))).thenReturn(List.of(first, second));
        mvc.perform(post("/api/v1/challenges").contentType("application/json").content("""
                {"title":"Array warmup","visibility":"PRIVATE","selection":{"tagsAll":["ARRAYS"],"difficulties":["EASY"],"supportedLanguage":"java","requestedQuestionCount":2}}
                """))
                .andExpect(status().isCreated()).andExpect(header().string("Location", org.hamcrest.Matchers.matchesPattern("/api/v1/challenges/[0-9a-f-]+")))
                .andExpect(jsonPath("$.lifecycleStatus").value("DRAFT")).andExpect(jsonPath("$.versionStatus").value("DRAFT"))
                .andExpect(jsonPath("$.resolvedQuestions.length()").value(2)).andExpect(jsonPath("$.selection.tagsAll[0]").value("arrays"))
                .andExpect(jsonPath("$.resolvedQuestions[0].questionId").exists()).andExpect(jsonPath("$.resolvedQuestions[0].questionVersionId").exists());
        assertThat(jdbc.queryForObject("select count(*) from challenge.challenges", Integer.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject("select count(*) from challenge.challenge_versions where status = 'DRAFT' and version_number = 1", Integer.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject("select count(*) from challenge.challenge_version_questions", Integer.class)).isEqualTo(2);
    }

    @Test void rejectsCreationWithoutEnoughCatalogQuestions() throws Exception {
        when(catalog.resolve(any(RuleBasedSelection.class))).thenReturn(List.of(new QuestionCatalogEntry(UUID.randomUUID(), UUID.randomUUID())));
        mvc.perform(post("/api/v1/challenges").contentType("application/json").content("""
                {"title":"Array warmup","visibility":"PUBLIC","selection":{"tagsAll":["arrays"],"requestedQuestionCount":2}}
                """))
                .andExpect(status().isUnprocessableEntity()).andExpect(jsonPath("$.code").value("QUESTION_SELECTION_INSUFFICIENT"));
        assertThat(jdbc.queryForObject("select count(*) from challenge.challenges", Integer.class)).isZero();
    }
}
