package com.mockarena.assessment;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mockarena.assessment.infrastructure.challenge.*;
import com.mockarena.assessment.domain.AssessmentVersion;
import com.mockarena.assessment.domain.Attempt;
import com.mockarena.assessment.application.AttemptStartEntitlementPort;
import com.mockarena.assessment.infrastructure.question.*;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import jakarta.persistence.EntityManager;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import java.util.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.reset;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = "assessment.security.enabled=false")
@AutoConfigureMockMvc @Testcontainers @Transactional
class AssessmentServiceIntegrationTest {
    @Container static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");
    @DynamicPropertySource static void database(DynamicPropertyRegistry registry) { registry.add("spring.datasource.url", postgres::getJdbcUrl); registry.add("spring.datasource.username", postgres::getUsername); registry.add("spring.datasource.password", postgres::getPassword); }
    @Autowired MockMvc mvc; @Autowired JdbcTemplate jdbc; @Autowired ObjectMapper json; @Autowired EntityManager entityManager;
    @MockitoBean ChallengeVersionCatalogClient challenges;
    @MockitoBean AttemptStartEntitlementPort entitlements;
    @MockitoBean QuestionCandidateContentClient questionContent;

    @BeforeEach void defaultCatalogueManifestResolution() {
        when(challenges.resolveManifests(any())).thenAnswer(invocation -> { List<UUID> ids = invocation.getArgument(0); return ids == null ? List.of() : ids.stream().map(id -> new ChallengeVersionManifest(UUID.randomUUID(), id, 1, List.of(new ChallengeQuestionReference(1, UUID.randomUUID(), UUID.randomUUID(), "MCQ")))).toList(); });
    }

    @Test void createsDraftAssessmentWithExactOrderedMixedChallengeManifest() throws Exception {
        ChallengeVersionReference mcq = reference(), coding = reference(), mixed = reference(); when(challenges.resolve(any())).thenReturn(List.of(mcq, coding, mixed));
        String body = mvc.perform(post("/api/v1/assessments").contentType("application/json").content(request(List.of(mcq.challengeVersionId(), coding.challengeVersionId(), mixed.challengeVersionId()))))
            .andExpect(status().isCreated()).andExpect(jsonPath("$.status").value("DRAFT")).andExpect(jsonPath("$.challenges.length()").value(3))
            .andExpect(jsonPath("$.challenges[0].challengeVersionId").value(mcq.challengeVersionId().toString())).andExpect(jsonPath("$.challenges[1].challengeVersionId").value(coding.challengeVersionId().toString())).andReturn().getResponse().getContentAsString();
        UUID assessmentId = UUID.fromString(json.readTree(body).get("assessmentId").asText());
        assertThat(jdbc.queryForObject("select created_by_user_id from assessment.assessments where id = ?", UUID.class, assessmentId)).isEqualTo(new UUID(0, 0));
        assertThat(jdbc.queryForObject("select count(*) from assessment.assessment_version_challenges", Integer.class)).isEqualTo(3);
    }

    @Test void rejectsMissingDraftAndRetiredChallengeVersionsAndUnavailableResolver() throws Exception {
        when(challenges.resolve(any())).thenThrow(new ChallengeVersionNotComposableException());
        mvc.perform(post("/api/v1/assessments").contentType("application/json").content(request(List.of(UUID.randomUUID())))).andExpect(status().isUnprocessableEntity()).andExpect(jsonPath("$.code").value("CHALLENGE_VERSION_NOT_COMPOSABLE"));
        reset(challenges);
        when(challenges.resolve(any())).thenThrow(new ChallengeServiceUnavailableException());
        mvc.perform(post("/api/v1/assessments").contentType("application/json").content(request(List.of(UUID.randomUUID())))).andExpect(status().isServiceUnavailable()).andExpect(jsonPath("$.code").value("CHALLENGE_SERVICE_UNAVAILABLE"));
    }

    @Test void validatesTimingAttemptAndResultReleasePolicies() throws Exception {
        ChallengeVersionReference reference = reference(); when(challenges.resolve(any())).thenReturn(List.of(reference));
        mvc.perform(post("/api/v1/assessments").contentType("application/json").content(request(List.of(reference.challengeVersionId())).replace("\"attemptDurationSeconds\":3600", "\"attemptDurationSeconds\":0"))).andExpect(status().isBadRequest());
        mvc.perform(post("/api/v1/assessments").contentType("application/json").content(request(List.of(reference.challengeVersionId())).replace("\"maxAttempts\":1", "\"maxAttempts\":0"))).andExpect(status().isBadRequest());
        mvc.perform(post("/api/v1/assessments").contentType("application/json").content(request(List.of(reference.challengeVersionId())).replace("\"policyCode\":\"MANUAL\",\"parameters\":{}", "\"policyCode\":\"SCHEDULED\",\"parameters\":{}"))).andExpect(status().isBadRequest());
        mvc.perform(post("/api/v1/assessments").contentType("application/json").content(request(List.of(reference.challengeVersionId())).replaceFirst("\\\"availableUntil\\\":\\\"[^\\\"]+\\\"", "\\\"availableUntil\\\":\\\"2000-09-11T00:00:00Z\\\""))).andExpect(status().isBadRequest());
    }

    @Test void establishesAvailabilityStartAndEffectiveDeadlineSemantics() {
        var content = new AssessmentVersion.Content("Timed", null, null, "STANDARD", "FIXED_DURATION", JsonNodeFactory.instance.objectNode(), java.time.Instant.parse("2026-01-01T10:00:00Z"), java.time.Instant.parse("2026-01-01T11:00:00Z"), 7_200, "MAX_ATTEMPTS", JsonNodeFactory.instance.objectNode().put("maxAttempts", 1), "IMMEDIATE", JsonNodeFactory.instance.objectNode());
        AssessmentVersion version = new AssessmentVersion(UUID.randomUUID(), UUID.randomUUID(), 1, content, java.time.Instant.now()); version.publish(java.time.Instant.now());
        assertThat(version.canStartAttemptAt(java.time.Instant.parse("2026-01-01T10:00:00Z"))).isTrue();
        assertThat(version.canStartAttemptAt(java.time.Instant.parse("2026-01-01T11:00:00Z"))).isFalse();
        assertThat(version.effectiveAttemptDeadline(java.time.Instant.parse("2026-01-01T10:30:00Z"))).isEqualTo(java.time.Instant.parse("2026-01-01T11:00:00Z"));
        version.close(java.time.Instant.now());
        assertThat(version.canStartAttemptAt(java.time.Instant.parse("2026-01-01T10:30:00Z"))).isFalse();
    }

    @Test void publishesFreezesManifestAndAllowsRevisionAndAssessmentClosing() throws Exception {
        ChallengeVersionReference first = reference(), second = reference(); when(challenges.resolve(any())).thenReturn(List.of(first), List.of(first), List.of(second));
        var created = json.readTree(mvc.perform(post("/api/v1/assessments").contentType("application/json").content(request(List.of(first.challengeVersionId())))).andReturn().getResponse().getContentAsString());
        String assessmentId = created.get("assessmentId").asText();
        mvc.perform(post("/api/v1/assessments/{id}/versions/1/publish", assessmentId).contentType("application/json").content("{\"expectedAssessmentVersion\":0,\"expectedVersion\":0}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("PUBLISHED"));
        assertThat(jdbc.queryForObject("select count(*) from assessment.public_assessment_catalogue where assessment_id = ?", Integer.class, UUID.fromString(assessmentId))).isEqualTo(1);
        mvc.perform(post("/api/v1/assessments/{id}/versions", assessmentId).contentType("application/json").content("{\"expectedAssessmentVersion\":1,\"content\":" + content(List.of(second.challengeVersionId())) + "}"))
            .andExpect(status().isCreated()).andExpect(jsonPath("$.versionNumber").value(2)).andExpect(jsonPath("$.status").value("DRAFT"));
        mvc.perform(post("/api/v1/assessments/{id}/close", assessmentId).contentType("application/json").content("{\"expectedAssessmentVersion\":2}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.lifecycleStatus").value("CLOSED"));
        assertThatThrownByDatabaseUpdate(assessmentId);
    }

    @Test void rejectsStaleWriteAndRetiresWithoutChangingManifest() throws Exception {
        ChallengeVersionReference reference = reference(); when(challenges.resolve(any())).thenReturn(List.of(reference), List.of(reference));
        var created = json.readTree(mvc.perform(post("/api/v1/assessments").contentType("application/json").content(request(List.of(reference.challengeVersionId())))).andReturn().getResponse().getContentAsString()); String id = created.get("assessmentId").asText();
        mvc.perform(post("/api/v1/assessments/{id}/versions/1/publish", id).contentType("application/json").content("{\"expectedAssessmentVersion\":9,\"expectedVersion\":0}")).andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("VERSION_CONFLICT"));
        mvc.perform(post("/api/v1/assessments/{id}/versions/1/publish", id).contentType("application/json").content("{\"expectedAssessmentVersion\":0,\"expectedVersion\":0}")).andExpect(status().isOk());
        mvc.perform(post("/api/v1/assessments/{id}/versions/1/retire", id).contentType("application/json").content("{\"expectedAssessmentVersion\":1,\"expectedVersion\":1}")).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("RETIRED"));
        assertThat(jdbc.queryForObject("select count(*) from assessment.assessment_version_challenges", Integer.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject("select current_published_version_id from assessment.assessments where id = ?", UUID.class, UUID.fromString(id))).isNull();
    }

    @Test void closesPublishedVersionWithoutRewritingItsHistoricalContent() throws Exception {
        ChallengeVersionReference reference = reference(); when(challenges.resolve(any())).thenReturn(List.of(reference), List.of(reference));
        var created = json.readTree(mvc.perform(post("/api/v1/assessments").contentType("application/json").content(request(List.of(reference.challengeVersionId())))).andReturn().getResponse().getContentAsString()); String id = created.get("assessmentId").asText();
        mvc.perform(post("/api/v1/assessments/{id}/versions/1/publish", id).contentType("application/json").content("{\"expectedAssessmentVersion\":0,\"expectedVersion\":0}")).andExpect(status().isOk());
        mvc.perform(post("/api/v1/assessments/{id}/versions/1/close", id).contentType("application/json").content("{\"expectedAssessmentVersion\":1,\"expectedVersion\":1}")).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("CLOSED"));
        assertThat(jdbc.queryForObject("select current_published_version_id from assessment.assessments where id = ?", UUID.class, UUID.fromString(id))).isNull();
        assertThatThrownByDatabaseUpdate(id);
    }

    @Test void startsIdOnlyMixedAttemptManifestAndResumesIdempotently() throws Exception {
        ChallengeVersionReference reference = reference();
        UUID reservation = UUID.randomUUID(), firstQuestion = UUID.randomUUID(), firstQuestionVersion = UUID.randomUUID(), secondQuestion = UUID.randomUUID(), secondQuestionVersion = UUID.randomUUID();
        when(challenges.resolve(any())).thenReturn(List.of(reference), List.of(reference));
        when(challenges.resolveManifests(any())).thenReturn(List.of(new ChallengeVersionManifest(reference.challengeId(), reference.challengeVersionId(), reference.challengeVersionNumber(), List.of(
                new ChallengeQuestionReference(1, firstQuestion, firstQuestionVersion, "MCQ"), new ChallengeQuestionReference(2, secondQuestion, secondQuestionVersion, "CODING")))));
        when(entitlements.reserve(any(), any(), any())).thenReturn(reservation);
        String assessmentId = publish(reference);

        String first = mvc.perform(post("/api/v1/assessments/{id}/versions/1/attempts", assessmentId).header("Idempotency-Key", "start-1"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.status").value("IN_PROGRESS")).andReturn().getResponse().getContentAsString();
        String attemptId = json.readTree(first).get("attemptId").asText();
        mvc.perform(post("/api/v1/assessments/{id}/versions/1/attempts", assessmentId).header("Idempotency-Key", "start-1"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.attemptId").value(attemptId));
        mvc.perform(post("/api/v1/assessments/{id}/versions/1/attempts", assessmentId).header("Idempotency-Key", "other-tab"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.attemptId").value(attemptId));
        assertThat(jdbc.queryForObject("select candidate_user_id from assessment.attempts where id = ?", UUID.class, UUID.fromString(attemptId))).isEqualTo(new UUID(0, 0));
        assertThat(jdbc.queryForList("select global_position, challenge_position, question_position, question_id, question_version_id, question_type_code from assessment.attempt_items where attempt_id = ? order by global_position", UUID.fromString(attemptId)))
                .extracting(row -> row.get("global_position"), row -> row.get("challenge_position"), row -> row.get("question_position"), row -> row.get("question_id"), row -> row.get("question_version_id"), row -> row.get("question_type_code"))
                .containsExactly(tuple(1, 1, 1, firstQuestion, firstQuestionVersion, "MCQ"), tuple(2, 1, 2, secondQuestion, secondQuestionVersion, "CODING"));
        org.mockito.Mockito.verify(entitlements).commit(reservation);
    }

    @Test void rejectsReusedIdempotencyKeyForAnotherAssessmentAndManifestFailures() throws Exception {
        ChallengeVersionReference first = reference(), second = reference(); when(challenges.resolve(any())).thenReturn(List.of(first), List.of(first), List.of(second), List.of(second));
        when(challenges.resolveManifests(any())).thenReturn(List.of(new ChallengeVersionManifest(first.challengeId(), first.challengeVersionId(), 1, List.of(new ChallengeQuestionReference(1, UUID.randomUUID(), UUID.randomUUID(), "MCQ")))));
        when(entitlements.reserve(any(), any(), any())).thenReturn(UUID.randomUUID());
        String one = publish(first), two = publish(second);
        mvc.perform(post("/api/v1/assessments/{id}/versions/1/attempts", one).header("Idempotency-Key", "reused")).andExpect(status().isCreated());
        mvc.perform(post("/api/v1/assessments/{id}/versions/1/attempts", two).header("Idempotency-Key", "reused")).andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("IDEMPOTENCY_KEY_REUSED"));
        org.mockito.Mockito.doThrow(new ChallengeVersionNotComposableException()).when(challenges).resolveManifests(any());
        mvc.perform(post("/api/v1/assessments/{id}/versions/1/attempts", two).header("Idempotency-Key", "missing-manifest")).andExpect(status().isUnprocessableEntity());
        org.mockito.Mockito.doThrow(new ChallengeServiceUnavailableException()).when(challenges).resolveManifests(any());
        mvc.perform(post("/api/v1/assessments/{id}/versions/1/attempts", two).header("Idempotency-Key", "unavailable-manifest")).andExpect(status().isServiceUnavailable());
    }

    @Test void expiresWhenDeadlineIsStrictlyBeforeNow() {
        Attempt attempt = new Attempt(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), 1, UUID.randomUUID(), java.time.Instant.parse("2026-01-01T10:00:00Z"), java.time.Instant.parse("2026-01-01T10:01:00Z"), UUID.randomUUID());
        assertThat(attempt.expireIfDue(java.time.Instant.parse("2026-01-01T10:01:00Z"))).isFalse();
        assertThat(attempt.expireIfDue(java.time.Instant.parse("2026-01-01T10:01:01Z"))).isTrue();
        assertThat(attempt.status().name()).isEqualTo("EXPIRED");
    }

    @Test void candidateOwnerReceivesOrderedSafeMixedContentWithoutPersistence() throws Exception {
        ChallengeVersionReference reference=reference(); UUID q1=UUID.randomUUID(),v1=UUID.randomUUID(),q2=UUID.randomUUID(),v2=UUID.randomUUID(),reservation=UUID.randomUUID();
        when(challenges.resolve(any())).thenReturn(List.of(reference),List.of(reference)); when(challenges.resolveManifests(any())).thenReturn(List.of(new ChallengeVersionManifest(reference.challengeId(),reference.challengeVersionId(),1,List.of(new ChallengeQuestionReference(1,q1,v1,"MCQ"),new ChallengeQuestionReference(2,q2,v2,"CODING")))));
        when(questionContent.resolve(any())).thenReturn(List.of(new QuestionCandidateContent(q1,v1,"MCQ","MCQ","Stem",List.of(new QuestionCandidateContent.Option("a","A"),new QuestionCandidateContent.Option("b","B")),null,null,null),new QuestionCandidateContent(q2,v2,"CODING","Code","Code stem",List.of(),"constraints",JsonNodeFactory.instance.arrayNode(),JsonNodeFactory.instance.arrayNode().add("JAVA")))); when(entitlements.reserve(any(),any(),any())).thenReturn(reservation);
        String assessment=publish(reference); String started=mvc.perform(post("/api/v1/assessments/{id}/versions/1/attempts",assessment).header("Idempotency-Key","content")).andReturn().getResponse().getContentAsString(); String attempt=json.readTree(started).get("attemptId").asText();
        mvc.perform(get("/api/v1/attempts/{id}/content",attempt)).andExpect(status().isOk()).andExpect(jsonPath("$[0].questionTypeCode").value("MCQ")).andExpect(jsonPath("$[1].questionTypeCode").value("CODING")).andExpect(jsonPath("$[0].correctOptionId").doesNotExist()).andExpect(jsonPath("$[1].hiddenTests").doesNotExist()).andExpect(jsonPath("$[1].scoringRules").doesNotExist()).andExpect(jsonPath("$[1].executionLimits").doesNotExist());
        assertThat(jdbc.queryForObject("select count(*) from information_schema.columns where table_schema='assessment' and table_name='attempt_items' and column_name='stem'",Integer.class)).isZero();
    }

    @Test void autosavesMixedResponsesInRouteOrderWithReplayAndStaleWriteProtection() throws Exception {
        String attempt = startMixedAttempt();
        UUID mcqMutation = UUID.randomUUID();
        String mcq = response("MCQ", "B", null, null, 0, mcqMutation);
        String saved = mvc.perform(put("/api/v1/attempts/{id}/responses/1", attempt).header("Idempotency-Key", "mcq-save").contentType("application/json").content(mcq))
            .andExpect(status().isOk()).andExpect(jsonPath("$.responseTypeCode").value("MCQ")).andExpect(jsonPath("$.selectedOptionId").value("B"))
            .andReturn().getResponse().getContentAsString();
        long firstVersion = json.readTree(saved).get("version").asLong();
        mvc.perform(put("/api/v1/attempts/{id}/responses/1", attempt).header("Idempotency-Key", "mcq-save").contentType("application/json").content(mcq))
            .andExpect(status().isOk()).andExpect(jsonPath("$.version").value(firstVersion));
        mvc.perform(put("/api/v1/attempts/{id}/responses/1", attempt).header("Idempotency-Key", "mcq-update").contentType("application/json").content(response("MCQ", "C", null, null, firstVersion, UUID.randomUUID())))
            .andExpect(status().isOk()).andExpect(jsonPath("$.version").value(firstVersion + 1));
        mvc.perform(put("/api/v1/attempts/{id}/responses/1", attempt).header("Idempotency-Key", "mcq-save").contentType("application/json").content(mcq))
            .andExpect(status().isOk()).andExpect(jsonPath("$.selectedOptionId").value("B")).andExpect(jsonPath("$.version").value(firstVersion));
        mvc.perform(put("/api/v1/attempts/{id}/responses/1", attempt).header("Idempotency-Key", "other-key").contentType("application/json").content(response("MCQ", "A", null, null, firstVersion, UUID.randomUUID())))
            .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("RESPONSE_VERSION_CONFLICT"));
        mvc.perform(put("/api/v1/attempts/{id}/responses/2", attempt).header("Idempotency-Key", "code-save").contentType("application/json").content(response("CODING", null, "JAVA", "class Solution { }", 0, UUID.randomUUID())))
            .andExpect(status().isOk()).andExpect(jsonPath("$.programmingLanguage").value("JAVA")).andExpect(jsonPath("$.sourceCode").value("class Solution { }"));
        mvc.perform(get("/api/v1/attempts/{id}/responses", attempt)).andExpect(status().isOk()).andExpect(jsonPath("$[0].globalPosition").value(1)).andExpect(jsonPath("$[0].selectedOptionId").value("C"))
            .andExpect(jsonPath("$[1].globalPosition").value(2)).andExpect(jsonPath("$[1].sourceCode").value("class Solution { }"))
            .andExpect(jsonPath("$[0].correctOptionId").doesNotExist()).andExpect(jsonPath("$[1].hiddenTests").doesNotExist()).andExpect(jsonPath("$[1].scoringRules").doesNotExist());
        assertThat(jdbc.queryForObject("select count(*) from assessment.attempt_item_responses where attempt_id = ?", Integer.class, UUID.fromString(attempt))).isEqualTo(2);
    }

    @Test void autosaveRejectsInvalidPayloadsPositionsIdempotencyReuseAndInactiveAttempts() throws Exception {
        String attempt = startMixedAttempt();
        mvc.perform(put("/api/v1/attempts/{id}/responses/3", attempt).header("Idempotency-Key", "position").contentType("application/json").content(response("MCQ", "A", null, null, 0, UUID.randomUUID())))
            .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
        mvc.perform(put("/api/v1/attempts/{id}/responses/1", attempt).header("Idempotency-Key", "wrong-type").contentType("application/json").content(response("CODING", null, "JAVA", "x", 0, UUID.randomUUID())))
            .andExpect(status().isBadRequest());
        mvc.perform(put("/api/v1/attempts/{id}/responses/1", attempt).header("Idempotency-Key", "missing-option").contentType("application/json").content(response("MCQ", null, null, null, 0, UUID.randomUUID())))
            .andExpect(status().isBadRequest());
        mvc.perform(put("/api/v1/attempts/{id}/responses/2", attempt).header("Idempotency-Key", "missing-language").contentType("application/json").content(response("CODING", null, null, "x", 0, UUID.randomUUID())))
            .andExpect(status().isBadRequest());
        mvc.perform(put("/api/v1/attempts/{id}/responses/2", attempt).header("Idempotency-Key", "missing-source").contentType("application/json").content(response("CODING", null, "JAVA", null, 0, UUID.randomUUID())))
            .andExpect(status().isBadRequest());
        UUID mutation = UUID.randomUUID();
        String first = response("MCQ", "A", null, null, 0, mutation);
        mvc.perform(put("/api/v1/attempts/{id}/responses/1", attempt).header("Idempotency-Key", "reused").contentType("application/json").content(first)).andExpect(status().isOk());
        mvc.perform(put("/api/v1/attempts/{id}/responses/1", attempt).header("Idempotency-Key", "reused").contentType("application/json").content(response("MCQ", "B", null, null, 0, UUID.randomUUID())))
            .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("IDEMPOTENCY_KEY_REUSED"));
        mvc.perform(put("/api/v1/attempts/{id}/responses/1", attempt).header("Idempotency-Key", "same-mutation-other-key").contentType("application/json").content(first)).andExpect(status().isOk());
        jdbc.update("update assessment.attempts set status='SUBMITTED' where id=?", UUID.fromString(attempt)); entityManager.clear();
        mvc.perform(put("/api/v1/attempts/{id}/responses/1", attempt).header("Idempotency-Key", "submitted").contentType("application/json").content(response("MCQ", "A", null, null, 0, UUID.randomUUID())))
            .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("INVALID_STATE"));
    }

    @Test void autosaveExpiresDueAttemptAndDoesNotExposeSourceInErrors() throws Exception {
        String attempt = startMixedAttempt();
        jdbc.update("update assessment.attempts set deadline_at = now() - interval '1 second' where id=?", UUID.fromString(attempt)); entityManager.clear();
        mvc.perform(put("/api/v1/attempts/{id}/responses/2", attempt).header("Idempotency-Key", "expired").contentType("application/json").content(response("CODING", null, "JAVA", "private candidate source", 0, UUID.randomUUID())))
            .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("INVALID_STATE"))
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("private candidate source"))));
        assertThat(jdbc.queryForObject("select status from assessment.attempts where id=?", String.class, UUID.fromString(attempt))).isEqualTo("EXPIRED");
    }

    private String startMixedAttempt() throws Exception {
        ChallengeVersionReference reference = reference(); UUID firstQuestion = UUID.randomUUID(), firstVersion = UUID.randomUUID(), secondQuestion = UUID.randomUUID(), secondVersion = UUID.randomUUID();
        when(challenges.resolve(any())).thenReturn(List.of(reference), List.of(reference));
        when(challenges.resolveManifests(any())).thenReturn(List.of(new ChallengeVersionManifest(reference.challengeId(), reference.challengeVersionId(), reference.challengeVersionNumber(), List.of(
            new ChallengeQuestionReference(1, firstQuestion, firstVersion, "MCQ"), new ChallengeQuestionReference(2, secondQuestion, secondVersion, "CODING")))));
        when(entitlements.reserve(any(), any(), any())).thenReturn(UUID.randomUUID());
        String assessment = publish(reference);
        String started = mvc.perform(post("/api/v1/assessments/{id}/versions/1/attempts", assessment).header("Idempotency-Key", "start-" + UUID.randomUUID()))
            .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return json.readTree(started).get("attemptId").asText();
    }

    private static String response(String type, String selectedOptionId, String programmingLanguage, String sourceCode, long expectedVersion, UUID mutationId) {
        String selected = selectedOptionId == null ? "null" : "\"" + selectedOptionId + "\"";
        String language = programmingLanguage == null ? "null" : "\"" + programmingLanguage + "\"";
        String source = sourceCode == null ? "null" : "\"" + sourceCode + "\"";
        return "{\"responseTypeCode\":\"" + type + "\",\"selectedOptionId\":" + selected + ",\"programmingLanguage\":" + language + ",\"sourceCode\":" + source + ",\"expectedResponseVersion\":" + expectedVersion + ",\"clientMutationId\":\"" + mutationId + "\"}";
    }

    private String publish(ChallengeVersionReference reference) throws Exception {
        var created = json.readTree(mvc.perform(post("/api/v1/assessments").contentType("application/json").content(request(List.of(reference.challengeVersionId())))).andReturn().getResponse().getContentAsString());
        String assessmentId = created.get("assessmentId").asText();
        mvc.perform(post("/api/v1/assessments/{id}/versions/1/publish", assessmentId).contentType("application/json").content("{\"expectedAssessmentVersion\":0,\"expectedVersion\":0}")).andExpect(status().isOk());
        return assessmentId;
    }

    private void assertThatThrownByDatabaseUpdate(String assessmentId) { org.assertj.core.api.Assertions.assertThatThrownBy(() -> jdbc.update("update assessment.assessment_versions set title = 'changed' where assessment_id = ? and version_number = 1", UUID.fromString(assessmentId))).hasMessageContaining("published assessment versions are immutable"); }
    private static ChallengeVersionReference reference() { return new ChallengeVersionReference(UUID.randomUUID(), UUID.randomUUID(), 1, "PUBLISHED"); }
    private static String request(List<UUID> ids) { return "{\"visibility\":\"PRIVATE\",\"content\":" + content(ids) + "}"; }
    private static String content(List<UUID> ids) { java.time.Instant now = java.time.Instant.now(); return "{\"title\":\"Mixed assessment\",\"description\":\"safe metadata only\",\"instructions\":\"Complete all challenges\",\"assessmentTypeCode\":\"STANDARD\",\"timingPolicy\":{\"policyCode\":\"FIXED_DURATION\",\"parameters\":{}},\"availableFrom\":\"" + now.minusSeconds(3600) + "\",\"availableUntil\":\"" + now.plusSeconds(86400) + "\",\"attemptDurationSeconds\":3600,\"attemptPolicy\":{\"policyCode\":\"MAX_ATTEMPTS\",\"parameters\":{\"maxAttempts\":1}},\"resultReleasePolicy\":{\"policyCode\":\"MANUAL\",\"parameters\":{}},\"challengeVersionIds\":[" + ids.stream().map(id -> "\"" + id + "\"").collect(java.util.stream.Collectors.joining(",")) + "]}"; }
}
