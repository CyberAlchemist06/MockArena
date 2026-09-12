package com.mockarena.question;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mockarena.question.domain.QuestionVersionRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.reset;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@Transactional
class QuestionServiceIntegrationTest {
    @Container static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");
    @DynamicPropertySource static void database(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url", postgres::getJdbcUrl); r.add("spring.datasource.username", postgres::getUsername); r.add("spring.datasource.password", postgres::getPassword);
    }
    @Autowired MockMvc mvc; @Autowired ObjectMapper json; @Autowired JdbcTemplate jdbc; @Autowired EntityManager entityManager;
    @MockitoSpyBean QuestionVersionRepository questionVersionRepository;
    @AfterEach void resetRepositorySpy() { reset(questionVersionRepository); }

    @Test void createsQuestionAndInitialVersionAtomically_andNeverReturnsHiddenTests() throws Exception {
        String body=validRequest(UUID.randomUUID());
        String response=mvc.perform(post("/api/v1/questions").contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isCreated())
            .andExpect(header().string("Location", org.hamcrest.Matchers.matchesPattern("/api/v1/questions/[0-9a-f-]+/versions/1")))
            .andExpect(jsonPath("$.questionId").exists())
            .andExpect(jsonPath("$.versionNumber").value(1))
            .andExpect(jsonPath("$.status").value("DRAFT"))
            .andExpect(jsonPath("$.title").value("Two Sum"))
            .andExpect(jsonPath("$.hiddenTests").doesNotExist())
            .andReturn().getResponse().getContentAsString();
        assertThatThrownBy(() -> json.readTree(response).required("hiddenTests")).isInstanceOf(IllegalArgumentException.class);
        String questionId=json.readTree(response).required("questionId").asText();
        entityManager.flush();
        Integer questionCount=jdbc.queryForObject("select count(*) from question.questions where id = ?", Integer.class, UUID.fromString(questionId));
        Integer versionCount=jdbc.queryForObject("select count(*) from question.question_versions where question_id = ? and version_number = 1", Integer.class, UUID.fromString(questionId));
        org.assertj.core.api.Assertions.assertThat(questionCount).isEqualTo(1);
        org.assertj.core.api.Assertions.assertThat(versionCount).isEqualTo(1);
        mvc.perform(post("/api/v1/questions/{id}/versions/1/publish",questionId).contentType(MediaType.APPLICATION_JSON).content("{\"expectedQuestionVersion\":0,\"expectedVersion\":0}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.hiddenTests").doesNotExist());
        assertThatThrownBy(() -> jdbc.update("update question.question_versions set title = 'changed' where question_id = ? and version_number = 1", UUID.fromString(questionId)))
            .hasMessageContaining("published question versions are immutable");
    }

    @Test void rejectsBlankTitleUsingStableValidationError() throws Exception {
        String body=validRequest(UUID.randomUUID()).replace("\"title\":\"Two Sum\"", "\"title\":\"   \"");
        assertValidationFailure(body);
    }

    @Test void rollsBackQuestionWhenInitialVersionPersistenceFails() throws Exception {
        doThrow(new IllegalStateException("Version persistence failed")).when(questionVersionRepository).save(any());
        mvc.perform(post("/api/v1/questions").contentType(MediaType.APPLICATION_JSON).content(validRequest(UUID.randomUUID())))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value("INVALID_STATE"));
        Integer questionCount=jdbc.queryForObject("select count(*) from question.questions", Integer.class);
        Integer versionCount=jdbc.queryForObject("select count(*) from question.question_versions", Integer.class);
        org.assertj.core.api.Assertions.assertThat(questionCount).isZero();
        org.assertj.core.api.Assertions.assertThat(versionCount).isZero();
    }

    @Test void rejectsMissingRequiredNestedContentUsingStableValidationError() throws Exception {
        String body=validRequest(UUID.randomUUID()).replace("\"examples\":[],", "");
        assertValidationFailure(body);
    }

    @Test void rejectsInvalidNestedFieldUsingStableValidationError() throws Exception {
        String body=validRequest(UUID.randomUUID()).replace("\"title\":\"Two Sum\"", "\"title\":\"%s\"".formatted("a".repeat(201)));
        assertValidationFailure(body);
    }

    @Test void normalizesTagsAndRejectsInvalidDifficulty() throws Exception {
        String body=validRequest(UUID.randomUUID()).replace("\"tags\":[\"arrays\"]", "\"tags\":[\" Arrays \",\"arrays\",\"HASHING\"]");
        String response=mvc.perform(post("/api/v1/questions").contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.tags[0]").value("arrays"))
            .andExpect(jsonPath("$.tags[1]").value("hashing"))
            .andExpect(jsonPath("$.difficulty").value("EASY"))
            .andReturn().getResponse().getContentAsString();
        UUID questionId=UUID.fromString(json.readTree(response).required("questionId").asText());
        entityManager.flush();
        assertThat(jdbc.queryForObject("select tags::text from question.question_versions where question_id = ?", String.class, questionId)).isEqualTo("[\"arrays\", \"hashing\"]");

        mvc.perform(post("/api/v1/questions").contentType(MediaType.APPLICATION_JSON).content(validRequest(UUID.randomUUID()).replace("\"difficulty\":\"EASY\"", "\"difficulty\":\"UNKNOWN\"")))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test void catalogReturnsOnlyPublishedCurrentVersionsAndOnlyApprovedFields() throws Exception {
        UUID questionId=UUID.randomUUID();
        UUID oldVersion=insertCatalogVersion(questionId, 1, "PUBLISHED", "Old", List.of("arrays"), "EASY", List.of("JAVA"));
        UUID currentVersion=insertCatalogVersion(questionId, 2, "PUBLISHED", "Current", List.of("arrays"), "EASY", List.of("JAVA"));
        markCurrent(questionId, currentVersion);
        UUID draftQuestion=UUID.randomUUID();
        UUID draftVersion=insertCatalogVersion(draftQuestion, 1, "DRAFT", "Draft", List.of("arrays"), "EASY", List.of("JAVA"));
        markCurrent(draftQuestion, draftVersion);

        String response=mvc.perform(post("/internal/v1/question-versions/resolve").contentType(MediaType.APPLICATION_JSON).content(catalogRequest("[\"arrays\"]", "[\"EASY\"]", "JAVA")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.entries.length()").value(1))
            .andExpect(jsonPath("$.entries[0].questionId").value(questionId.toString()))
            .andExpect(jsonPath("$.entries[0].questionVersionId").value(currentVersion.toString()))
            .andExpect(jsonPath("$.entries[0].versionNumber").value(2))
            .andExpect(jsonPath("$.entries[0].prompt").doesNotExist())
            .andExpect(jsonPath("$.entries[0].constraints").doesNotExist())
            .andExpect(jsonPath("$.entries[0].examples").doesNotExist())
            .andExpect(jsonPath("$.entries[0].visibleTests").doesNotExist())
            .andExpect(jsonPath("$.entries[0].hiddenTests").doesNotExist())
            .andExpect(jsonPath("$.entries[0].scoringRules").doesNotExist())
            .andExpect(jsonPath("$.entries[0].executionLimits").doesNotExist())
            .andReturn().getResponse().getContentAsString();
        assertThat(json.readTree(response).required("entries").get(0).properties().stream().map(Map.Entry::getKey).collect(Collectors.toSet()))
            .isEqualTo(Set.of("questionId", "questionVersionId", "versionNumber", "title", "tags", "difficulty", "supportedLanguages"));
        assertThat(oldVersion).isNotEqualTo(currentVersion);
    }

    @Test void catalogFiltersTagsWithAndSemanticsDifficultyAndSupportedLanguage() throws Exception {
        UUID matchingQuestion=UUID.randomUUID();
        UUID matchingVersion=insertCatalogVersion(matchingQuestion, 1, "PUBLISHED", "Matching", List.of("arrays", "hashing"), "MEDIUM", List.of("JAVA", "PYTHON"));
        markCurrent(matchingQuestion, matchingVersion);
        UUID missingTagQuestion=UUID.randomUUID();
        UUID missingTagVersion=insertCatalogVersion(missingTagQuestion, 1, "PUBLISHED", "Missing tag", List.of("arrays"), "MEDIUM", List.of("JAVA"));
        markCurrent(missingTagQuestion, missingTagVersion);
        UUID wrongDifficultyQuestion=UUID.randomUUID();
        UUID wrongDifficultyVersion=insertCatalogVersion(wrongDifficultyQuestion, 1, "PUBLISHED", "Wrong difficulty", List.of("arrays", "hashing"), "HARD", List.of("JAVA"));
        markCurrent(wrongDifficultyQuestion, wrongDifficultyVersion);
        UUID wrongLanguageQuestion=UUID.randomUUID();
        UUID wrongLanguageVersion=insertCatalogVersion(wrongLanguageQuestion, 1, "PUBLISHED", "Wrong language", List.of("arrays", "hashing"), "MEDIUM", List.of("PYTHON"));
        markCurrent(wrongLanguageQuestion, wrongLanguageVersion);

        mvc.perform(post("/internal/v1/question-versions/resolve").contentType(MediaType.APPLICATION_JSON).content(catalogRequest("[\"ARRAYS\",\"hashing\"]", "[\"EASY\",\"MEDIUM\"]", "java")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.entries.length()").value(1))
            .andExpect(jsonPath("$.entries[0].questionVersionId").value(matchingVersion.toString()));
    }

    @Test void catalogReturnsEmptyResultsWhenNothingMatches() throws Exception {
        UUID questionId=UUID.randomUUID();
        UUID versionId=insertCatalogVersion(questionId, 1, "PUBLISHED", "Graph", List.of("graphs"), "HARD", List.of("JAVA"));
        markCurrent(questionId, versionId);
        mvc.perform(post("/internal/v1/question-versions/resolve").contentType(MediaType.APPLICATION_JSON).content(catalogRequest("[\"arrays\"]", "[\"EASY\"]", "JAVA")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.entries").isEmpty());
    }

    private void assertValidationFailure(String body) throws Exception {
        mvc.perform(post("/api/v1/questions").contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
            .andExpect(jsonPath("$.message").value("Request validation failed"));
        Integer questionCount=jdbc.queryForObject("select count(*) from question.questions", Integer.class);
        Integer versionCount=jdbc.queryForObject("select count(*) from question.question_versions", Integer.class);
        org.assertj.core.api.Assertions.assertThat(questionCount).isZero();
        org.assertj.core.api.Assertions.assertThat(versionCount).isZero();
    }

    private static String validRequest(UUID ownerUserId) {
        return """
          {"ownerUserId":"%s","content":{"title":"Two Sum","tags":["arrays"],"difficulty":"EASY","prompt":"Find pair","constraintsText":"n >= 2","examples":[],"supportedLanguages":["JAVA"],"visibleTests":[{"input":"[2,7]","output":"[0,1]"}],"hiddenTests":[{"input":"secret-input","output":"secret-output"}],"scoringRules":{"points":100},"executionLimits":{"timeMs":1000}}}
          """.formatted(ownerUserId);
    }

    private String catalogRequest(String tagsAll, String difficulties, String supportedLanguage) {
        return "{\"tagsAll\":%s,\"difficulties\":%s,\"supportedLanguage\":\"%s\",\"limit\":20}".formatted(tagsAll, difficulties, supportedLanguage);
    }

    private UUID insertCatalogVersion(UUID questionId, int versionNumber, String status, String title, List<String> tags, String difficulty, List<String> supportedLanguages) throws Exception {
        UUID versionId=UUID.randomUUID();
        jdbc.update("insert into question.questions (id, owner_user_id, lifecycle_status, version, created_at, updated_at) values (?, ?, 'PUBLISHED', 0, now(), now()) on conflict (id) do nothing", questionId, UUID.randomUUID());
        jdbc.update("insert into question.question_versions (id, question_id, version_number, status, title, tags, difficulty, prompt, supported_languages, version, created_at, updated_at) values (?, ?, ?, ?, ?, cast(? as jsonb), ?, 'prompt', cast(? as jsonb), 0, now(), now())", versionId, questionId, versionNumber, status, title, json.writeValueAsString(tags), difficulty, json.writeValueAsString(supportedLanguages));
        return versionId;
    }

    private void markCurrent(UUID questionId, UUID versionId) {
        jdbc.update("update question.questions set current_version_id = ? where id = ?", versionId, questionId);
    }
}
