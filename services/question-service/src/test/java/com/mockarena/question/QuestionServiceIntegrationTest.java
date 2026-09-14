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

@SpringBootTest(properties = "question.security.enabled=false")
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
    @BeforeEach void isolateDatabase() {
        // Testcontainers-only fixture reset. TRUNCATE does not invoke the production
        // row-level immutability triggers that ordinary DELETE operations must obey.
        jdbc.execute("TRUNCATE TABLE question.question_version_historical_generic_metadata, question.question_version_taxonomy, question.question_versions, question.questions RESTART IDENTITY CASCADE");
    }
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

    @Test void createsValidMcqAndNeverExposesItsCorrectAnswer() throws Exception {
        String response = mvc.perform(post("/api/v1/questions").contentType(MediaType.APPLICATION_JSON).content(validMcqRequest(UUID.randomUUID())))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.questionType").value("MCQ"))
            .andExpect(jsonPath("$.options.length()").value(3))
            .andExpect(jsonPath("$.options[1].id").value("inorder"))
            .andExpect(jsonPath("$.correctOptionId").doesNotExist())
            .andExpect(jsonPath("$.explanation").doesNotExist())
            .andExpect(jsonPath("$.hiddenTests").doesNotExist())
            .andReturn().getResponse().getContentAsString();
        assertThat(response).doesNotContain("inorder traversal is sorted");
        assertThat(response).doesNotContain("correctOptionId");
    }

    @Test void candidateContentReturnsExactPublishedAndRetiredMixedVersionsWithoutProtectedData() throws Exception {
        UUID codingQuestion = UUID.randomUUID(), mcqQuestion = UUID.randomUUID();
        String coding = mvc.perform(post("/api/v1/questions").contentType(MediaType.APPLICATION_JSON).content(validRequest(codingQuestion))).andReturn().getResponse().getContentAsString();
        String mcq = mvc.perform(post("/api/v1/questions").contentType(MediaType.APPLICATION_JSON).content(validMcqRequest(mcqQuestion))).andReturn().getResponse().getContentAsString();
        var codingTree=json.readTree(coding); var mcqTree=json.readTree(mcq); UUID codingVersion=UUID.fromString(codingTree.get("id").asText()), mcqVersion=UUID.fromString(mcqTree.get("id").asText());
        mvc.perform(post("/api/v1/questions/{id}/versions/1/publish",codingTree.get("questionId").asText()).contentType(MediaType.APPLICATION_JSON).content("{\"expectedQuestionVersion\":0,\"expectedVersion\":0}")).andExpect(status().isOk());
        // Question Service deliberately has no retirement command yet.  Model a
        // historical retired fixture before publication rather than attempting to
        // mutate an immutable published version.
        jdbc.update("update question.question_versions set status='RETIRED' where id=? and status='DRAFT'", mcqVersion);
        entityManager.clear();
        String body=mvc.perform(post("/internal/v3/question-versions/candidate-content").contentType(MediaType.APPLICATION_JSON).content("{\"questionVersionIds\":[\""+mcqVersion+"\",\""+codingVersion+"\"]}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.entries[0].questionTypeCode").value("MCQ")).andExpect(jsonPath("$.entries[0].options.length()").value(3)).andExpect(jsonPath("$.entries[1].questionTypeCode").value("CODING"))
                .andExpect(jsonPath("$.entries[0].correctOptionId").doesNotExist()).andExpect(jsonPath("$.entries[0].explanation").doesNotExist()).andExpect(jsonPath("$.entries[1].hiddenTests").doesNotExist()).andExpect(jsonPath("$.entries[1].scoringRules").doesNotExist()).andExpect(jsonPath("$.entries[1].executionLimits").doesNotExist()).andReturn().getResponse().getContentAsString();
        assertThat(body).doesNotContain("inorder traversal is sorted").doesNotContain("secret-input").doesNotContain("correctOptionId");
        mvc.perform(post("/internal/v3/question-versions/candidate-content").contentType(MediaType.APPLICATION_JSON).content("{\"questionVersionIds\":[\""+UUID.randomUUID()+"\"]}")).andExpect(status().isNotFound());
    }

    @Test void protectedEvaluationDataUsesExactHistoricalMcqVersionsOnly() throws Exception {
        UUID publishedQuestion = UUID.randomUUID(), retiredQuestion = UUID.randomUUID();
        var publishedTree = json.readTree(mvc.perform(post("/api/v1/questions").contentType(MediaType.APPLICATION_JSON).content(validMcqRequest(publishedQuestion))).andReturn().getResponse().getContentAsString());
        publishedQuestion = UUID.fromString(publishedTree.get("questionId").asText());
        UUID publishedVersion = UUID.fromString(publishedTree.get("id").asText());
        mvc.perform(post("/api/v1/questions/{id}/versions/1/publish", publishedQuestion).contentType(MediaType.APPLICATION_JSON).content("{\"expectedQuestionVersion\":0,\"expectedVersion\":0}"))
            .andExpect(status().isOk());

        var retiredTree = json.readTree(mvc.perform(post("/api/v1/questions").contentType(MediaType.APPLICATION_JSON).content(validMcqRequest(retiredQuestion))).andReturn().getResponse().getContentAsString());
        UUID retiredVersion = UUID.fromString(retiredTree.get("id").asText());
        jdbc.update("update question.question_versions set status='RETIRED' where id=?", retiredVersion);
        entityManager.clear();

        String body = mvc.perform(post("/internal/v1/question-versions/evaluation-data").contentType(MediaType.APPLICATION_JSON)
                .content("{\"questionVersionIds\":[\"" + retiredVersion + "\",\"" + publishedVersion + "\"]}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.entries[0].questionVersionId").value(retiredVersion.toString()))
            .andExpect(jsonPath("$.entries[1].questionVersionId").value(publishedVersion.toString()))
            .andExpect(jsonPath("$.entries[0].questionTypeCode").value("MCQ"))
            .andExpect(jsonPath("$.entries[0].correctOptionId").value("inorder"))
            .andExpect(jsonPath("$.entries[0].scoringPolicy").exists())
            .andReturn().getResponse().getContentAsString();
        assertThat(body).contains("correctOptionId");

        mvc.perform(post("/internal/v1/question-versions/evaluation-data").contentType(MediaType.APPLICATION_JSON)
                .content("{\"questionVersionIds\":[\"" + UUID.randomUUID() + "\"]}"))
            .andExpect(status().isNotFound());
        String draft = mvc.perform(post("/api/v1/questions").contentType(MediaType.APPLICATION_JSON).content(validMcqRequest(UUID.randomUUID()))).andReturn().getResponse().getContentAsString();
        UUID draftVersion = UUID.fromString(json.readTree(draft).get("id").asText());
        mvc.perform(post("/internal/v1/question-versions/evaluation-data").contentType(MediaType.APPLICATION_JSON)
                .content("{\"questionVersionIds\":[\"" + draftVersion + "\"]}"))
            .andExpect(status().isNotFound());
    }

    @Test void rejectsMcqWithFewerThanTwoOptionsInvalidCorrectOptionOrDuplicateOptionIds() throws Exception {
        assertInvalidContent(validMcqRequest(UUID.randomUUID()).replace("{\"id\":\"preorder\",\"text\":\"Preorder\"},{\"id\":\"inorder\",\"text\":\"Inorder\"},{\"id\":\"postorder\",\"text\":\"Postorder\"}", "{\"id\":\"preorder\",\"text\":\"Preorder\"}"));
        assertInvalidContent(validMcqRequest(UUID.randomUUID()).replace("\"correctOptionId\":\"inorder\"", "\"correctOptionId\":\"missing\""));
        assertInvalidContent(validMcqRequest(UUID.randomUUID()).replace("\"id\":\"postorder\"", "\"id\":\"inorder\""));
    }

    @Test void rejectsMixedTypeSpecificContent() throws Exception {
        assertInvalidContent(validMcqRequest(UUID.randomUUID()).replace("\"options\":", "\"examples\":[],\"options\":"));
        assertInvalidContent(validRequest(UUID.randomUUID()).replace("\"executionLimits\":{\"timeMs\":1000}", "\"executionLimits\":{\"timeMs\":1000},\"options\":[{\"id\":\"a\",\"text\":\"A\"},{\"id\":\"b\",\"text\":\"B\"}],\"correctOptionId\":\"a\""));
    }

    @Test void rejectsTypeChangeOnUpdateButAllowsMcqRevision() throws Exception {
        String created = mvc.perform(post("/api/v1/questions").contentType(MediaType.APPLICATION_JSON).content(validRequest(UUID.randomUUID())))
            .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        UUID questionId = UUID.fromString(json.readTree(created).required("questionId").asText());
        mvc.perform(put("/api/v1/questions/{id}/versions/1", questionId).contentType(MediaType.APPLICATION_JSON)
                .content("{\"expectedVersion\":0,\"content\":" + contentOnly(validMcqRequest(UUID.randomUUID())) + "}"))
            .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("INVALID_STATE"));
        mvc.perform(post("/api/v1/questions/{id}/versions", questionId).contentType(MediaType.APPLICATION_JSON)
                .content("{\"expectedQuestionVersion\":0,\"content\":" + contentOnly(validMcqRequest(UUID.randomUUID())) + "}"))
            .andExpect(status().isCreated()).andExpect(jsonPath("$.versionNumber").value(2)).andExpect(jsonPath("$.questionType").value("MCQ"));
    }

    @Test void catalogReturnsQuestionTypeButNeverMcqContent() throws Exception {
        String created = mvc.perform(post("/api/v1/questions").contentType(MediaType.APPLICATION_JSON).content(validMcqRequest(UUID.randomUUID())))
            .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        UUID questionId = UUID.fromString(json.readTree(created).required("questionId").asText());
        mvc.perform(post("/api/v1/questions/{id}/versions/1/publish", questionId).contentType(MediaType.APPLICATION_JSON).content("{\"expectedQuestionVersion\":0,\"expectedVersion\":0}"))
            .andExpect(status().isOk());
        mvc.perform(post("/internal/v1/question-versions/resolve").contentType(MediaType.APPLICATION_JSON).content(catalogRequest("[\"trees\"]", "[\"EASY\"]", null)))
            .andExpect(status().isOk()).andExpect(jsonPath("$.entries[0].questionType").value("MCQ"))
            .andExpect(jsonPath("$.entries[0].prompt").doesNotExist()).andExpect(jsonPath("$.entries[0].options").doesNotExist())
            .andExpect(jsonPath("$.entries[0].correctOptionId").doesNotExist()).andExpect(jsonPath("$.entries[0].explanation").doesNotExist());
    }

    @Test void publishedMcqIsImmutableInDatabase() throws Exception {
        String created = mvc.perform(post("/api/v1/questions").contentType(MediaType.APPLICATION_JSON).content(validMcqRequest(UUID.randomUUID())))
            .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        UUID questionId = UUID.fromString(json.readTree(created).required("questionId").asText());
        mvc.perform(post("/api/v1/questions/{id}/versions/1/publish", questionId).contentType(MediaType.APPLICATION_JSON).content("{\"expectedQuestionVersion\":0,\"expectedVersion\":0}"))
            .andExpect(status().isOk());
        assertThatThrownBy(() -> jdbc.update("update question.question_versions set correct_option_id = 'preorder' where question_id = ?", questionId))
            .hasMessageContaining("published question versions are immutable");
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
            .isEqualTo(Set.of("questionId", "questionVersionId", "versionNumber", "title", "tags", "difficulty", "questionType", "supportedLanguages"));
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

    @Test void v2CatalogFiltersGenericMetadataAndNeverSerializesProtectedContent() throws Exception {
        String created = mvc.perform(post("/api/v1/questions").contentType(MediaType.APPLICATION_JSON).content(validRequest(UUID.randomUUID())))
            .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        String questionId = json.readTree(created).required("questionId").asText();
        mvc.perform(post("/api/v1/questions/{id}/versions/1/publish", questionId).contentType(MediaType.APPLICATION_JSON)
                .content("{\"expectedQuestionVersion\":0,\"expectedVersion\":0}"))
            .andExpect(status().isOk());

        String request = """
                {"taxonomyAll":[{"scheme":"topic","code":"arrays"}],
                 "questionTypeCodes":["CODING"],
                 "difficultyProfiles":[{"scheme":"mockarena-v1","code":"EASY"}],
                 "contentLocales":["en"],"programmingLanguages":["JAVA"],"limit":1}
                """;
        mvc.perform(post("/internal/v2/question-versions/resolve").contentType(MediaType.APPLICATION_JSON).content(request))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.entries.length()").value(1))
            .andExpect(jsonPath("$.entries[0].questionTypeCode").value("CODING"))
            .andExpect(jsonPath("$.entries[0].contentLocale").value("en"))
            .andExpect(jsonPath("$.entries[0].difficultyProfile.scheme").value("mockarena-v1"))
            .andExpect(jsonPath("$.entries[0].prompt").doesNotExist())
            .andExpect(jsonPath("$.entries[0].hiddenTests").doesNotExist())
            .andExpect(jsonPath("$.entries[0].correctOptionId").doesNotExist())
            .andExpect(jsonPath("$.entries[0].defaultScoringPolicy").doesNotExist());
    }

    @Test void v2CatalogUsesOpaqueCursorForStableCompleteKeysetTraversal() throws Exception {
        List<UUID> questionIds = java.util.stream.IntStream.range(0, 5)
                .mapToObj(index -> UUID.nameUUIDFromBytes(("cursor-question-" + index).getBytes())).toList();
        for (UUID questionId : questionIds) {
            UUID versionId = insertCatalogVersion(questionId, 1, "PUBLISHED", "Cursor " + questionId, List.of("arrays"), "EASY", List.of("JAVA"));
            markCurrent(questionId, versionId);
        }

        var first = json.readTree(mvc.perform(post("/internal/v2/question-versions/resolve").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"limit\":2}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.entries.length()").value(2))
                .andExpect(jsonPath("$.nextCursor").isNotEmpty()).andReturn().getResponse().getContentAsString());
        String firstCursor = first.get("nextCursor").asText();
        var second = json.readTree(mvc.perform(post("/internal/v2/question-versions/resolve").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"limit\":2,\"cursor\":\"" + firstCursor + "\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.entries.length()").value(2))
                .andExpect(jsonPath("$.nextCursor").isNotEmpty()).andReturn().getResponse().getContentAsString());
        var last = json.readTree(mvc.perform(post("/internal/v2/question-versions/resolve").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"limit\":2,\"cursor\":\"" + second.get("nextCursor").asText() + "\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.entries.length()").value(1))
                .andExpect(jsonPath("$.nextCursor").value(org.hamcrest.Matchers.nullValue())).andReturn().getResponse().getContentAsString());

        List<String> returned = java.util.stream.Stream.of(first, second, last).flatMap(page -> java.util.stream.StreamSupport.stream(page.get("entries").spliterator(), false))
                .map(entry -> entry.get("questionId").asText() + ":" + entry.get("questionVersionId").asText()).toList();
        assertThat(returned).doesNotHaveDuplicates();
        assertThat(returned).hasSize(5);
        assertThat(returned).isSorted(); // canonical traversal is questionId then questionVersionId.

        mvc.perform(post("/internal/v2/question-versions/resolve").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"limit\":2,\"cursor\":\"not-an-opaque-cursor\"}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
        mvc.perform(post("/internal/v2/question-versions/resolve").contentType(MediaType.APPLICATION_JSON).content("{\"limit\":2}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.entries.length()").value(2));
    }

    @Test void historicalGenericMetadataIsReadWithoutRewritingPublishedQuestionVersions() throws Exception {
        String created = mvc.perform(post("/api/v1/questions").contentType(MediaType.APPLICATION_JSON).content(validRequest(UUID.randomUUID())))
            .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        var createdTree = json.readTree(created);
        UUID versionId = UUID.fromString(createdTree.required("id").asText());
        String questionId = createdTree.required("questionId").asText();
        jdbc.update("""
                insert into question.question_version_historical_generic_metadata
                    (question_version_id, question_type_code, content_locale, difficulty_scheme, difficulty_code, programming_languages, default_scoring_policy)
                values (?, 'CODING', 'en', 'mockarena-v1', 'HARD', cast(? as jsonb), cast(? as jsonb))
                """, versionId, "[\"PYTHON\"]", "{\"policyCode\":\"TEST_CASES\",\"parameters\":{\"points\":100}}");
        mvc.perform(post("/api/v1/questions/{id}/versions/1/publish", questionId).contentType(MediaType.APPLICATION_JSON)
                .content("{\"expectedQuestionVersion\":0,\"expectedVersion\":0}"))
            .andExpect(status().isOk());

        String request = """
                {"taxonomyAll":[{"scheme":"topic","code":"arrays"}],
                 "questionTypeCodes":["CODING"],
                 "difficultyProfiles":[{"scheme":"mockarena-v1","code":"HARD"}],
                 "contentLocales":["en"],"programmingLanguages":["PYTHON"],"limit":1}
                """;
        mvc.perform(post("/internal/v2/question-versions/resolve").contentType(MediaType.APPLICATION_JSON).content(request))
            .andExpect(status().isOk()).andExpect(jsonPath("$.entries[0].questionVersionId").value(versionId.toString()));
        mvc.perform(post("/internal/v3/question-versions/candidate-content").contentType(MediaType.APPLICATION_JSON)
                .content("{\"questionVersionIds\":[\"" + versionId + "\"]}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.entries[0].programmingLanguages[0]").value("PYTHON"));
        assertThatThrownBy(() -> jdbc.update("update question.question_version_historical_generic_metadata set difficulty_code = 'EASY' where question_version_id = ?", versionId))
            .hasMessageContaining("historical question version generic metadata is immutable");
    }

    @Test void v3DefaultsExistingStyleRowsToCoding() throws Exception {
        UUID questionId = UUID.randomUUID();
        UUID versionId = insertCatalogVersion(questionId, 1, "PUBLISHED", "Legacy coding", List.of("arrays"), "EASY", List.of("JAVA"));
        assertThat(jdbc.queryForObject("select question_type from question.question_versions where id = ?", String.class, versionId)).isEqualTo("CODING");
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
          {"ownerUserId":"%s","content":{"title":"Two Sum","tags":["arrays"],"difficulty":"EASY","questionType":"CODING","prompt":"Find pair","constraintsText":"n >= 2","examples":[],"supportedLanguages":["JAVA"],"visibleTests":[{"input":"[2,7]","output":"[0,1]"}],"hiddenTests":[{"input":"secret-input","output":"secret-output"}],"scoringRules":{"points":100},"executionLimits":{"timeMs":1000}}}
          """.formatted(ownerUserId);
    }

    private static String validMcqRequest(UUID ownerUserId) {
        return """
          {"ownerUserId":"%s","content":{"title":"BST Traversal","tags":["trees"],"difficulty":"EASY","questionType":"MCQ","prompt":"Which traversal is sorted?","options":[{"id":"preorder","text":"Preorder"},{"id":"inorder","text":"Inorder"},{"id":"postorder","text":"Postorder"}],"correctOptionId":"inorder","explanation":"inorder traversal is sorted"}}
          """.formatted(ownerUserId);
    }

    private static String contentOnly(String request) { return request.substring(request.indexOf("\"content\":") + 10, request.length() - 1); }

    private void assertInvalidContent(String body) throws Exception {
        mvc.perform(post("/api/v1/questions").contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    private String catalogRequest(String tagsAll, String difficulties, String supportedLanguage) {
        String language = supportedLanguage == null ? "null" : "\"%s\"".formatted(supportedLanguage);
        return "{\"tagsAll\":%s,\"difficulties\":%s,\"supportedLanguage\":%s,\"limit\":20}".formatted(tagsAll, difficulties, language);
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
