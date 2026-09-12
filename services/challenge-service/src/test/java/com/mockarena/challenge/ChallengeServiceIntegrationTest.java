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
import org.springframework.transaction.annotation.Transactional;
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

@SpringBootTest(properties = "challenge.security.enabled=false")
@AutoConfigureMockMvc
@Testcontainers
@Transactional
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
                {"title":"Array warmup","visibility":"PRIVATE","selection":{"taxonomyAll":[{"scheme":"content-domain","code":"DSA"},{"scheme":"topic","code":"ARRAYS"}],"questionTypeCodes":["coding"],"difficultyProfiles":[{"scheme":"mockarena-v1","code":"easy"}],"contentLocales":["EN"],"programmingLanguages":["java"],"requestedQuestionCount":2}}
                """))
                .andExpect(status().isCreated()).andExpect(header().string("Location", org.hamcrest.Matchers.matchesPattern("/api/v1/challenges/[0-9a-f-]+")))
                .andExpect(jsonPath("$.lifecycleStatus").value("DRAFT")).andExpect(jsonPath("$.versionStatus").value("DRAFT"))
                .andExpect(jsonPath("$.resolvedQuestions.length()").value(2)).andExpect(jsonPath("$.selection.taxonomyAll[1].code").value("arrays"))
                .andExpect(jsonPath("$.resolvedQuestions[0].questionId").exists()).andExpect(jsonPath("$.resolvedQuestions[0].questionVersionId").exists());
        assertThat(jdbc.queryForObject("select count(*) from challenge.challenges", Integer.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject("select count(*) from challenge.challenge_versions where status = 'DRAFT' and version_number = 1", Integer.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject("select count(*) from challenge.challenge_version_questions", Integer.class)).isEqualTo(2);
        assertThat(jdbc.queryForObject("select taxonomy_all::text from challenge.challenge_versions", String.class)).contains("content-domain").contains("arrays");
        assertThat(jdbc.queryForObject("select question_type_codes::text from challenge.challenge_versions", String.class)).isEqualTo("[\"CODING\"]");
        assertThat(jdbc.queryForObject("select tags_all::text from challenge.challenge_versions", String.class)).isEqualTo("[]");
    }

    @Test void rejectsCreationWithoutEnoughCatalogQuestions() throws Exception {
        when(catalog.resolve(any(RuleBasedSelection.class))).thenReturn(List.of(new QuestionCatalogEntry(UUID.randomUUID(), UUID.randomUUID())));
        mvc.perform(post("/api/v1/challenges").contentType("application/json").content("""
                {"title":"Array warmup","visibility":"PUBLIC","selection":{"taxonomyAll":[{"scheme":"topic","code":"arrays"}],"requestedQuestionCount":2}}
                """))
            .andExpect(status().isUnprocessableEntity()).andExpect(jsonPath("$.code").value("QUESTION_SELECTION_INSUFFICIENT"));
        assertThat(jdbc.queryForObject("select count(*) from challenge.challenges", Integer.class)).isZero();
    }

    @Test void publishesDraftWithAuthoritativeManifestAndResolvesOnlySafeVersionMetadata() throws Exception {
        QuestionCatalogEntry preview = new QuestionCatalogEntry(UUID.randomUUID(), UUID.randomUUID());
        QuestionCatalogEntry first = new QuestionCatalogEntry(UUID.randomUUID(), UUID.randomUUID());
        QuestionCatalogEntry second = new QuestionCatalogEntry(UUID.randomUUID(), UUID.randomUUID());
        when(catalog.resolve(any(RuleBasedSelection.class))).thenReturn(List.of(preview), List.of(first, second));
        String created = mvc.perform(post("/api/v1/challenges").contentType("application/json").content(request(1)))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        String challengeId = new com.fasterxml.jackson.databind.ObjectMapper().readTree(created).get("challengeId").asText();
        mvc.perform(post("/api/v1/challenges/{id}/versions/1/publish", challengeId).contentType("application/json")
                .content("{\"expectedChallengeVersion\":0,\"expectedVersion\":0}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.versionStatus").value("PUBLISHED"))
                .andExpect(jsonPath("$.resolvedQuestions.length()").value(1));
        assertThat(jdbc.queryForObject("select current_published_version_id from challenge.challenges where id = ?", UUID.class, UUID.fromString(challengeId))).isNotNull();
        mvc.perform(post("/internal/v1/challenge-versions/resolve").contentType("application/json")
                .content("{\"challengeVersionIds\":[\"" + jdbc.queryForObject("select current_published_version_id from challenge.challenges where id = ?", UUID.class, UUID.fromString(challengeId)) + "\"]}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.entries[0].challengeId").value(challengeId))
                .andExpect(jsonPath("$.entries[0].questionId").doesNotExist()).andExpect(jsonPath("$.entries[0].selection").doesNotExist());
    }

    @Test void resolvesPublishedManifestInExactPersistedOrderWithOnlySafeRoutingMetadata() throws Exception {
        QuestionCatalogEntry mcq = new QuestionCatalogEntry(UUID.randomUUID(), UUID.randomUUID(), "MCQ");
        QuestionCatalogEntry coding = new QuestionCatalogEntry(UUID.randomUUID(), UUID.randomUUID(), "CODING");
        when(catalog.resolve(any(RuleBasedSelection.class))).thenReturn(List.of(mcq, coding));
        String created = mvc.perform(post("/api/v1/challenges").contentType("application/json").content(request(2)))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        String challengeId = new com.fasterxml.jackson.databind.ObjectMapper().readTree(created).get("challengeId").asText();
        mvc.perform(post("/api/v1/challenges/{id}/versions/1/publish", challengeId).contentType("application/json")
                .content("{\"expectedChallengeVersion\":0,\"expectedVersion\":0}"))
                .andExpect(status().isOk());
        UUID versionId = jdbc.queryForObject("select current_published_version_id from challenge.challenges where id = ?", UUID.class, UUID.fromString(challengeId));
        var expected = jdbc.queryForList("select position, question_id, question_version_id, question_type_code from challenge.challenge_version_questions where challenge_version_id = ? order by position", versionId);

        mvc.perform(post("/internal/v2/challenge-versions/manifests").contentType("application/json")
                .content("{\"challengeVersionIds\":[\"" + versionId + "\"]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.entries[0].challengeId").value(challengeId))
                .andExpect(jsonPath("$.entries[0].challengeVersionId").value(versionId.toString()))
                .andExpect(jsonPath("$.entries[0].questions.length()").value(2))
                .andExpect(jsonPath("$.entries[0].questions[0].position").value(expected.get(0).get("position")))
                .andExpect(jsonPath("$.entries[0].questions[0].questionId").value(expected.get(0).get("question_id").toString()))
                .andExpect(jsonPath("$.entries[0].questions[0].questionVersionId").value(expected.get(0).get("question_version_id").toString()))
                .andExpect(jsonPath("$.entries[0].questions[0].questionTypeCode").value(expected.get(0).get("question_type_code")))
                .andExpect(jsonPath("$.entries[0].status").doesNotExist())
                .andExpect(jsonPath("$.entries[0].questions[0].title").doesNotExist())
                .andExpect(jsonPath("$.entries[0].questions[0].prompt").doesNotExist())
                .andExpect(jsonPath("$.entries[0].questions[0].options").doesNotExist())
                .andExpect(jsonPath("$.entries[0].questions[0].correctOptionId").doesNotExist())
                .andExpect(jsonPath("$.entries[0].questions[0].hiddenTests").doesNotExist())
                .andExpect(jsonPath("$.entries[0].questions[0].scoringRules").doesNotExist());

        // No Question Service call is involved: the returned route is historical manifest data.
        when(catalog.resolve(any(RuleBasedSelection.class))).thenReturn(List.of(new QuestionCatalogEntry(UUID.randomUUID(), UUID.randomUUID(), "MCQ")));
        mvc.perform(post("/internal/v2/challenge-versions/manifests").contentType("application/json")
                .content("{\"challengeVersionIds\":[\"" + versionId + "\"]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.entries[0].questions[0].questionVersionId").value(expected.get(0).get("question_version_id").toString()));
    }

    @Test void rejectsDraftRetiredIncompleteAndMissingManifestResolution() throws Exception {
        QuestionCatalogEntry entry = new QuestionCatalogEntry(UUID.randomUUID(), UUID.randomUUID(), "MCQ");
        when(catalog.resolve(any(RuleBasedSelection.class))).thenReturn(List.of(entry));
        var draft = new com.fasterxml.jackson.databind.ObjectMapper().readTree(mvc.perform(post("/api/v1/challenges").contentType("application/json").content(request(1))).andReturn().getResponse().getContentAsString());
        String challengeId = draft.get("challengeId").asText(), draftVersionId = draft.get("challengeVersionId").asText();
        mvc.perform(post("/internal/v2/challenge-versions/manifests").contentType("application/json").content("{\"challengeVersionIds\":[\"" + draftVersionId + "\"]}"))
                .andExpect(status().isUnprocessableEntity());
        mvc.perform(post("/api/v1/challenges/{id}/versions/1/publish", challengeId).contentType("application/json").content("{\"expectedChallengeVersion\":0,\"expectedVersion\":0}"))
                .andExpect(status().isOk());
        UUID published = jdbc.queryForObject("select current_published_version_id from challenge.challenges where id = ?", UUID.class, UUID.fromString(challengeId));
        mvc.perform(post("/api/v1/challenges/{id}/versions/1/retire", challengeId).contentType("application/json").content("{\"expectedChallengeVersion\":1,\"expectedVersion\":1}"))
                .andExpect(status().isOk());
        mvc.perform(post("/internal/v2/challenge-versions/manifests").contentType("application/json").content("{\"challengeVersionIds\":[\"" + published + "\"]}"))
                .andExpect(status().isUnprocessableEntity());
        mvc.perform(post("/internal/v2/challenge-versions/manifests").contentType("application/json").content("{\"challengeVersionIds\":[\"" + UUID.randomUUID() + "\"]}"))
                .andExpect(status().isNotFound());

        UUID incomplete = UUID.randomUUID();
        jdbc.update("insert into challenge.challenge_versions (id, challenge_id, version_number, status, title, requested_question_count, selection_seed, version, created_at, updated_at) values (?, ?, 2, 'DRAFT', 'Incomplete', 2, ?, 0, now(), now())", incomplete, UUID.fromString(challengeId), UUID.randomUUID());
        jdbc.update("insert into challenge.challenge_version_questions (challenge_version_id, position, question_id, question_version_id, question_type_code) values (?, 1, ?, ?, 'MCQ')", incomplete, UUID.randomUUID(), UUID.randomUUID());
        jdbc.update("update challenge.challenge_versions set status = 'PUBLISHED' where id = ?", incomplete);
        mvc.perform(post("/internal/v2/challenge-versions/manifests").contentType("application/json").content("{\"challengeVersionIds\":[\"" + incomplete + "\"]}"))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test void rejectsInsufficientPublicationAndLeavesDraftUnchanged() throws Exception {
        QuestionCatalogEntry entry = new QuestionCatalogEntry(UUID.randomUUID(), UUID.randomUUID());
        when(catalog.resolve(any(RuleBasedSelection.class))).thenReturn(List.of(entry), List.of());
        String created = mvc.perform(post("/api/v1/challenges").contentType("application/json").content(request(1))).andReturn().getResponse().getContentAsString();
        String challengeId = new com.fasterxml.jackson.databind.ObjectMapper().readTree(created).get("challengeId").asText();
        mvc.perform(post("/api/v1/challenges/{id}/versions/1/publish", challengeId).contentType("application/json")
                .content("{\"expectedChallengeVersion\":0,\"expectedVersion\":0}"))
                .andExpect(status().isUnprocessableEntity()).andExpect(jsonPath("$.code").value("QUESTION_SELECTION_INSUFFICIENT"));
        assertThat(jdbc.queryForObject("select status from challenge.challenge_versions where challenge_id = ?", String.class, UUID.fromString(challengeId))).isEqualTo("DRAFT");
    }

    @Test void retiresPublishedVersionWithoutArchivingChallenge() throws Exception {
        QuestionCatalogEntry entry = new QuestionCatalogEntry(UUID.randomUUID(), UUID.randomUUID());
        when(catalog.resolve(any(RuleBasedSelection.class))).thenReturn(List.of(entry));
        String created = mvc.perform(post("/api/v1/challenges").contentType("application/json").content(request(1))).andReturn().getResponse().getContentAsString();
        String challengeId = new com.fasterxml.jackson.databind.ObjectMapper().readTree(created).get("challengeId").asText();
        mvc.perform(post("/api/v1/challenges/{id}/versions/1/publish", challengeId).contentType("application/json").content("{\"expectedChallengeVersion\":0,\"expectedVersion\":0}")).andExpect(status().isOk());
        mvc.perform(post("/api/v1/challenges/{id}/versions/1/retire", challengeId).contentType("application/json").content("{\"expectedChallengeVersion\":1,\"expectedVersion\":1}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.versionStatus").value("RETIRED"));
        assertThat(jdbc.queryForObject("select current_published_version_id from challenge.challenges where id = ?", UUID.class, UUID.fromString(challengeId))).isNull();
        assertThat(jdbc.queryForObject("select lifecycle_status from challenge.challenges where id = ?", String.class, UUID.fromString(challengeId))).isEqualTo("PUBLISHED");
    }

    @Test void rejectsStalePublicationAndDraftInternalResolution() throws Exception {
        QuestionCatalogEntry entry = new QuestionCatalogEntry(UUID.randomUUID(), UUID.randomUUID());
        when(catalog.resolve(any(RuleBasedSelection.class))).thenReturn(List.of(entry));
        String created = mvc.perform(post("/api/v1/challenges").contentType("application/json").content(request(1))).andReturn().getResponse().getContentAsString();
        var tree = new com.fasterxml.jackson.databind.ObjectMapper().readTree(created);
        String challengeId = tree.get("challengeId").asText(), versionId = tree.get("challengeVersionId").asText();
        mvc.perform(post("/api/v1/challenges/{id}/versions/1/publish", challengeId).contentType("application/json").content("{\"expectedChallengeVersion\":9,\"expectedVersion\":0}"))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("VERSION_CONFLICT"));
        mvc.perform(post("/internal/v1/challenge-versions/resolve").contentType("application/json").content("{\"challengeVersionIds\":[\"" + versionId + "\"]}"))
                .andExpect(status().isUnprocessableEntity());
        mvc.perform(post("/internal/v1/challenge-versions/resolve").contentType("application/json").content("{\"challengeVersionIds\":[\"" + UUID.randomUUID() + "\"]}"))
                .andExpect(status().isNotFound());
    }

    @Test void leavesDraftUnchangedWhenQuestionCatalogIsUnavailableDuringPublication() throws Exception {
        QuestionCatalogEntry entry = new QuestionCatalogEntry(UUID.randomUUID(), UUID.randomUUID());
        when(catalog.resolve(any(RuleBasedSelection.class))).thenReturn(List.of(entry)).thenThrow(new com.mockarena.challenge.question.QuestionCatalogUnavailableException());
        String created = mvc.perform(post("/api/v1/challenges").contentType("application/json").content(request(1))).andReturn().getResponse().getContentAsString();
        String challengeId = new com.fasterxml.jackson.databind.ObjectMapper().readTree(created).get("challengeId").asText();
        mvc.perform(post("/api/v1/challenges/{id}/versions/1/publish", challengeId).contentType("application/json").content("{\"expectedChallengeVersion\":0,\"expectedVersion\":0}"))
                .andExpect(status().isServiceUnavailable());
        assertThat(jdbc.queryForObject("select status from challenge.challenge_versions where challenge_id = ?", String.class, UUID.fromString(challengeId))).isEqualTo("DRAFT");
    }

    private static String request(int count) { return """
            {"title":"Generic challenge","visibility":"PUBLIC","selection":{"taxonomyAll":[{"scheme":"topic","code":"trees"}],"questionTypeCodes":["CODING"],"difficultyProfiles":[{"scheme":"mockarena-v1","code":"MEDIUM"}],"contentLocales":["en"],"programmingLanguages":["JAVA"],"requestedQuestionCount":%d}}
            """.formatted(count); }
}
