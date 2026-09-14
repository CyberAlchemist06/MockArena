package com.mockarena.assessment;

import com.mockarena.assessment.infrastructure.challenge.ChallengeVersionCatalogClient;
import com.mockarena.assessment.infrastructure.question.QuestionCandidateContentClient;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.*;
import java.time.Instant;
import java.util.*;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = "assessment.security.enabled=false") @AutoConfigureMockMvc @Testcontainers @org.springframework.transaction.annotation.Transactional
class PublicAssessmentCatalogueIntegrationTest {
    @Container static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");
    @DynamicPropertySource static void database(DynamicPropertyRegistry registry) { registry.add("spring.datasource.url", postgres::getJdbcUrl); registry.add("spring.datasource.username", postgres::getUsername); registry.add("spring.datasource.password", postgres::getPassword); }
    @Autowired MockMvc mvc; @Autowired JdbcTemplate jdbc;
    @MockitoBean ChallengeVersionCatalogClient challenges; @MockitoBean QuestionCandidateContentClient questions;

    @Test void anonymouslyListsOnlyCurrentPublicPublishedAssessmentsAndUsesSafeFields() throws Exception {
        UUID publicId = seed("Public", "findable description", "PUBLIC", "PUBLISHED", "PUBLISHED", Instant.now().minusSeconds(60), null, Instant.parse("2026-09-12T10:00:00Z"));
        seed("Draft", "x", "PUBLIC", "DRAFT", "PUBLISHED", null, null, Instant.now()); seed("Private", "x", "PRIVATE", "PUBLISHED", "PUBLISHED", null, null, Instant.now()); seed("Shared", "x", "SHARED", "PUBLISHED", "PUBLISHED", null, null, Instant.now()); seed("Organisation", "x", "ORGANIZATION_ONLY", "PUBLISHED", "PUBLISHED", null, null, Instant.now()); seed("Closed", "x", "PUBLIC", "PUBLISHED", "CLOSED", null, null, Instant.now()); seed("Retired", "x", "PUBLIC", "PUBLISHED", "RETIRED", null, null, Instant.now());
        mvc.perform(get("/api/v1/public/assessments")).andExpect(status().isOk()).andExpect(jsonPath("$.items.length()").value(1)).andExpect(jsonPath("$.items[0].assessmentId").value(publicId.toString())).andExpect(jsonPath("$.items[0].questionCount").value(3)).andExpect(jsonPath("$.items[0].questionTypeCounts.MCQ").value(2)).andExpect(jsonPath("$.items[0].assessmentVersionId").doesNotExist()).andExpect(jsonPath("$.items[0].challengeId").doesNotExist()).andExpect(jsonPath("$.items[0].questionId").doesNotExist()).andExpect(jsonPath("$.items[0].attemptPolicy").doesNotExist());
        verifyNoInteractions(challenges, questions);
    }

    @Test void supportsAvailabilitySearchTypeAndDetailSafety() throws Exception {
        UUID open = seed("Java assessment", "backend basics", "PUBLIC", "PUBLISHED", "PUBLISHED", Instant.now().minusSeconds(60), Instant.now().plusSeconds(3600), Instant.parse("2026-09-12T10:00:00Z"));
        UUID upcoming = seed("Future", "java description", "PUBLIC", "PUBLISHED", "PUBLISHED", Instant.now().plusSeconds(3600), null, Instant.parse("2026-09-12T11:00:00Z"));
        mvc.perform(get("/api/v1/public/assessments?availability=OPEN&q=backend&assessmentTypeCode=standard")).andExpect(status().isOk()).andExpect(jsonPath("$.items.length()").value(1)).andExpect(jsonPath("$.items[0].assessmentId").value(open.toString()));
        mvc.perform(get("/api/v1/public/assessments?availability=UPCOMING")).andExpect(status().isOk()).andExpect(jsonPath("$.items[0].assessmentId").value(upcoming.toString()));
        mvc.perform(get("/api/v1/public/assessments/" + open)).andExpect(status().isOk()).andExpect(jsonPath("$.instructionsSummary").value("Safe instructions")).andExpect(jsonPath("$.attemptPolicy.maxAttempts").value(2)).andExpect(jsonPath("$.challengeVersionIds").doesNotExist()).andExpect(jsonPath("$.questionTypeCounts.CODING").value(1)).andExpect(jsonPath("$.resultRelease.parameters").doesNotExist());
        verifyNoInteractions(challenges, questions);
    }

    @Test void paginatesDeterministicallyAndRejectsInvalidCursorsAndPageSizes() throws Exception {
        UUID first = seed("A", "x", "PUBLIC", "PUBLISHED", "PUBLISHED", null, null, Instant.parse("2026-09-12T12:00:00Z")); seed("B", "x", "PUBLIC", "PUBLISHED", "PUBLISHED", null, null, Instant.parse("2026-09-12T11:00:00Z"));
        String body = mvc.perform(get("/api/v1/public/assessments?availability=ALL&pageSize=1")).andExpect(status().isOk()).andExpect(jsonPath("$.items[0].assessmentId").value(first.toString())).andReturn().getResponse().getContentAsString();
        String cursor = new com.fasterxml.jackson.databind.ObjectMapper().readTree(body).get("nextCursor").asText();
        mvc.perform(get("/api/v1/public/assessments?availability=ALL&pageSize=1&cursor=" + cursor)).andExpect(status().isOk()).andExpect(jsonPath("$.items[0].title").value("B"));
        mvc.perform(get("/api/v1/public/assessments?availability=OPEN&pageSize=1&cursor=" + cursor)).andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
        mvc.perform(get("/api/v1/public/assessments?cursor=not-a-cursor")).andExpect(status().isBadRequest()); mvc.perform(get("/api/v1/public/assessments?pageSize=51")).andExpect(status().isBadRequest());
    }

    @Test void hidesPrivateAndNonexistentDetailsWithTheSameNotFoundResponse() throws Exception {
        UUID privateId = seed("Private", "x", "PRIVATE", "PUBLISHED", "PUBLISHED", null, null, Instant.now());
        mvc.perform(get("/api/v1/public/assessments/" + privateId)).andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("ASSESSMENT_NOT_FOUND"));
        mvc.perform(get("/api/v1/public/assessments/" + UUID.randomUUID())).andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("ASSESSMENT_NOT_FOUND"));
    }

    private UUID seed(String title, String description, String visibility, String lifecycle, String versionStatus, Instant from, Instant until, Instant publishedAt) {
        UUID assessmentId = UUID.randomUUID(), versionId = UUID.randomUUID();
        jdbc.update("insert into assessment.assessments (id,created_by_user_id,visibility,lifecycle_status,current_published_version_id,version,created_at,updated_at) values (?,?,?, ?,?,0,now(),now())", assessmentId, UUID.randomUUID(), visibility, lifecycle, versionId);
        jdbc.update("insert into assessment.assessment_versions (id,assessment_id,version_number,status,title,description,instructions,assessment_type_code,timing_policy_code,timing_policy_parameters,available_from,available_until,attempt_duration_seconds,attempt_policy_code,attempt_policy_parameters,result_release_policy_code,result_release_policy_parameters,version,created_at,updated_at) values (?,?,1,?,?,?,'Safe instructions','STANDARD','FIXED_DURATION','{}'::jsonb,?,?,3600,'MAX_ATTEMPTS','{\"maxAttempts\":2}'::jsonb,'IMMEDIATE','{}'::jsonb,0,now(),now())", versionId, assessmentId, versionStatus, title, description, timestamp(from), timestamp(until));
        jdbc.update("insert into assessment.public_assessment_catalogue (assessment_version_id,assessment_id,version_number,title,description,instructions_summary,assessment_type_code,timing_policy_code,attempt_duration_seconds,available_from,available_until,max_attempts,result_release_policy_code,result_release_at,question_count,question_type_counts,published_at) values (?,?,1,?,?,?,'STANDARD','FIXED_DURATION',3600,?,?,2,'IMMEDIATE',null,3,'{\"MCQ\":2,\"CODING\":1}'::jsonb,?)", versionId, assessmentId, title, description, "Safe instructions", timestamp(from), timestamp(until), timestamp(publishedAt));
        return assessmentId;
    }
    private static java.sql.Timestamp timestamp(Instant value) { return value == null ? null : java.sql.Timestamp.from(value); }
}
