package com.mockarena.assessment;

import com.mockarena.assessment.infrastructure.question.QuestionEvaluationDataClient;
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

import java.util.UUID;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = "assessment.security.enabled=false")
@AutoConfigureMockMvc
@Testcontainers
@Transactional
class AttemptResultReadIntegrationTest {
    @Container static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");
    @DynamicPropertySource static void database(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;
    @MockitoBean QuestionEvaluationDataClient questionEvaluation;

    @Test void submittedAttemptWithoutResultIsPendingAndReadOnly() throws Exception {
        UUID attemptId = fixture("IMMEDIATE", "{}", false, false);

        mvc.perform(get("/api/v1/attempts/{attemptId}/result", attemptId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.evaluationStatus").value("PENDING"))
            .andExpect(jsonPath("$.score").isEmpty())
            .andExpect(jsonPath("$.items").isArray())
            .andExpect(jsonPath("$.items").isEmpty());

        verifyNoInteractions(questionEvaluation);
    }

    @Test void immediateFinalResultExposesOnlyDerivedFacts() throws Exception {
        UUID attemptId = fixture("IMMEDIATE", "{}", true, false);

        mvc.perform(get("/api/v1/attempts/{attemptId}/result", attemptId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.evaluationStatus").value("EVALUATED"))
            .andExpect(jsonPath("$.score").value(1))
            .andExpect(jsonPath("$.maxScore").value(1))
            .andExpect(jsonPath("$.percentage").value(100.0000))
            .andExpect(jsonPath("$.items[0].outcome").value("CORRECT"))
            .andExpect(jsonPath("$.correctOptionId").doesNotExist())
            .andExpect(jsonPath("$.items[0].correctOptionId").doesNotExist())
            .andExpect(jsonPath("$.items[0].scoringPolicy").doesNotExist())
            .andExpect(jsonPath("$.items[0].sourceCode").doesNotExist());

        verifyNoInteractions(questionEvaluation);
    }

    @Test void scheduledResultIsOpaqueBeforeReleaseAndVisibleAfterRelease() throws Exception {
        UUID hidden = fixture("SCHEDULED", "{\"releaseAt\":\"2999-01-01T00:00:00Z\"}", true, false);
        mvc.perform(get("/api/v1/attempts/{attemptId}/result", hidden))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value("RESULT_NOT_RELEASED"))
            .andExpect(jsonPath("$.score").doesNotExist());

        UUID visible = fixture("SCHEDULED", "{\"releaseAt\":\"2000-01-01T00:00:00Z\"}", true, true);
        mvc.perform(get("/api/v1/attempts/{attemptId}/result", visible))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.evaluationStatus").value("PARTIALLY_EVALUATED"))
            .andExpect(jsonPath("$.score").isEmpty())
            .andExpect(jsonPath("$.items[0].outcome").value("CORRECT"))
            .andExpect(jsonPath("$.items[1].evaluationStatus").value("PENDING"));
    }

    private UUID fixture(String releaseCode, String releaseParameters, boolean withResult, boolean mixed) {
        UUID assessmentId = UUID.randomUUID(), versionId = UUID.randomUUID(), attemptId = UUID.randomUUID();
        UUID owner = new UUID(0, 0);
        jdbc.update("insert into assessment.assessments (id,created_by_user_id,visibility,lifecycle_status,current_published_version_id,version,created_at,updated_at) values (?,?, 'PRIVATE','PUBLISHED',?,0,now(),now())", assessmentId, owner, versionId);
        jdbc.update("insert into assessment.assessment_versions (id,assessment_id,version_number,status,title,assessment_type_code,timing_policy_code,timing_policy_parameters,attempt_policy_code,attempt_policy_parameters,result_release_policy_code,result_release_policy_parameters,version,created_at,updated_at) values (?,?,1,'PUBLISHED','Result test','STANDARD','UNTIMED','{}'::jsonb,'MAX_ATTEMPTS','{\"maxAttempts\":1}'::jsonb,?,?::jsonb,0,now(),now())", versionId, assessmentId, releaseCode, releaseParameters);
        jdbc.update("insert into assessment.attempts (id,assessment_id,assessment_version_id,assessment_version_number,candidate_user_id,status,started_at,submitted_at,version,created_at,updated_at) values (?,?,?,1,?,'SUBMITTED',now(),now(),0,now(),now())", attemptId, assessmentId, versionId, owner);
        insertItem(attemptId, 1, "MCQ");
        if (mixed) insertItem(attemptId, 2, "CODING");
        if (withResult) {
            String aggregate = mixed ? "PARTIALLY_EVALUATED" : "EVALUATED";
            jdbc.update("insert into assessment.attempt_results (attempt_id,evaluation_status,raw_score,max_score,percentage,evaluated_at,released_at,version,created_at,updated_at) values (?,?,?,?,?,now(),null,0,now(),now())", attemptId, aggregate, mixed ? null : 1, mixed ? null : 1, mixed ? null : new java.math.BigDecimal("100.0000"));
            jdbc.update("insert into assessment.attempt_item_results (attempt_id,global_position,question_id,question_version_id,question_type_code,evaluation_status,outcome,awarded_score,max_score,evaluated_at) select attempt_id,global_position,question_id,question_version_id,question_type_code,'EVALUATED','CORRECT',1,1,now() from assessment.attempt_items where attempt_id=? and global_position=1", attemptId);
            if (mixed) jdbc.update("insert into assessment.attempt_item_results (attempt_id,global_position,question_id,question_version_id,question_type_code,evaluation_status,outcome,awarded_score,max_score,evaluated_at) select attempt_id,global_position,question_id,question_version_id,question_type_code,'PENDING',null,null,null,null from assessment.attempt_items where attempt_id=? and global_position=2", attemptId);
        }
        return attemptId;
    }

    private void insertItem(UUID attemptId, int position, String type) {
        jdbc.update("insert into assessment.attempt_items (attempt_id,global_position,challenge_id,challenge_version_id,challenge_position,question_id,question_version_id,question_position,question_type_code) values (?,?,?,?,?,?,?,?,?)", attemptId, position, UUID.randomUUID(), UUID.randomUUID(), 1, UUID.randomUUID(), UUID.randomUUID(), position, type);
    }
}
