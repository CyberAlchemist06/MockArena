package com.mockarena.assessment;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mockarena.assessment.infrastructure.challenge.*;
import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jwt.*;
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
import org.testcontainers.junit.jupiter.*;
import java.nio.file.*;
import java.security.*;
import java.security.interfaces.RSAPrivateKey;
import java.time.Instant;
import java.util.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest @AutoConfigureMockMvc @Testcontainers
class AssessmentJwtSecurityIntegrationTest {
    private static final String ISSUER = "mockarena-identity", AUDIENCE = "mockarena-api";
    private static final KeyPair KEY_PAIR = keyPair(); private static final Path PUBLIC_KEY_PATH = publicKey();
    @Container static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");
    @DynamicPropertySource static void properties(DynamicPropertyRegistry registry) { registry.add("spring.datasource.url", postgres::getJdbcUrl); registry.add("spring.datasource.username", postgres::getUsername); registry.add("spring.datasource.password", postgres::getPassword); registry.add("assessment.security.jwt.public-key-path", () -> PUBLIC_KEY_PATH.toString()); registry.add("assessment.security.jwt.issuer", () -> ISSUER); registry.add("assessment.security.jwt.audience", () -> AUDIENCE); registry.add("assessment.workload-auth.snapshot.token", () -> "assessment-test-workload-token"); }
    @Autowired MockMvc mvc; @Autowired ObjectMapper json; @Autowired JdbcTemplate jdbc; @MockitoBean ChallengeVersionCatalogClient challenges;

    @Test void validJwtDerivesCreatorAndIgnoresClientCreator() throws Exception {
        UUID subject = UUID.randomUUID(), malicious = UUID.randomUUID(), versionId = UUID.randomUUID(); when(challenges.resolve(any())).thenReturn(List.of(new ChallengeVersionReference(UUID.randomUUID(), versionId, 1, "PUBLISHED")));
        String response = mvc.perform(post("/api/v1/assessments").header("Authorization", "Bearer " + token(subject, ISSUER, AUDIENCE, Instant.now().plusSeconds(60), KEY_PAIR)).contentType("application/json").content(request(versionId, malicious))).andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        UUID assessmentId = UUID.fromString(json.readTree(response).get("assessmentId").asText()); assertThat(jdbc.queryForObject("select created_by_user_id from assessment.assessments where id = ?", UUID.class, assessmentId)).isEqualTo(subject);
    }
    @Test void missingTokenAndInvalidTokensAreRejectedWhileHealthIsPublic() throws Exception {
        UUID versionId = UUID.randomUUID(); mvc.perform(post("/api/v1/assessments").contentType("application/json").content(request(versionId, UUID.randomUUID()))).andExpect(status().isUnauthorized()).andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
        assertUnauthorized(token(UUID.randomUUID(), ISSUER, AUDIENCE, Instant.now().minusSeconds(1), KEY_PAIR), versionId); assertUnauthorized(token(UUID.randomUUID(), "wrong", AUDIENCE, Instant.now().plusSeconds(60), KEY_PAIR), versionId); assertUnauthorized(token(UUID.randomUUID(), ISSUER, "wrong", Instant.now().plusSeconds(60), KEY_PAIR), versionId); assertUnauthorized(token(UUID.randomUUID(), ISSUER, AUDIENCE, Instant.now().plusSeconds(60), keyPair()), versionId);
        mvc.perform(get("/actuator/health")).andExpect(status().isOk());
    }
    @Test void publicCatalogueDoesNotRequireJwtWhileOtherAssessmentRoutesDo() throws Exception {
        UUID assessmentId = UUID.randomUUID(), versionId = UUID.randomUUID();
        jdbc.update("insert into assessment.assessments (id,created_by_user_id,visibility,lifecycle_status,current_published_version_id,version,created_at,updated_at) values (?,?, 'PUBLIC','PUBLISHED',?,0,now(),now())", assessmentId, UUID.randomUUID(), versionId);
        jdbc.update("insert into assessment.assessment_versions (id,assessment_id,version_number,status,title,assessment_type_code,timing_policy_code,timing_policy_parameters,attempt_policy_code,attempt_policy_parameters,result_release_policy_code,result_release_policy_parameters,version,created_at,updated_at) values (?,?,1,'PUBLISHED','Public','STANDARD','UNTIMED','{}'::jsonb,'MAX_ATTEMPTS','{\"maxAttempts\":1}'::jsonb,'IMMEDIATE','{}'::jsonb,0,now(),now())", versionId, assessmentId);
        jdbc.update("insert into assessment.public_assessment_catalogue (assessment_version_id,assessment_id,version_number,title,assessment_type_code,timing_policy_code,max_attempts,result_release_policy_code,question_count,question_type_counts,published_at) values (?,?,1,'Public','STANDARD','UNTIMED',1,'IMMEDIATE',1,'{\"MCQ\":1}'::jsonb,now())", versionId, assessmentId);
        mvc.perform(get("/api/v1/public/assessments")).andExpect(status().isOk()).andExpect(jsonPath("$.items[0].assessmentId").value(assessmentId.toString()));
        mvc.perform(get("/api/v1/assessments/" + assessmentId)).andExpect(status().isUnauthorized());
    }
    @Test void autosaveRequiresJwtAndEnforcesAttemptOwnership() throws Exception {
        UUID attemptId = UUID.randomUUID(), owner = UUID.randomUUID(), other = UUID.randomUUID();
        jdbc.update("insert into assessment.attempts (id, assessment_id, assessment_version_id, assessment_version_number, candidate_user_id, status, started_at, deadline_at, version, created_at, updated_at) values (?, ?, ?, 1, ?, 'IN_PROGRESS', now(), null, 0, now(), now())", attemptId, UUID.randomUUID(), UUID.randomUUID(), owner);
        jdbc.update("insert into assessment.attempt_items (attempt_id, global_position, challenge_id, challenge_version_id, challenge_position, question_id, question_version_id, question_position, question_type_code) values (?, 1, ?, ?, 1, ?, ?, 1, 'MCQ')", attemptId, UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID());
        String body = "{\"responseTypeCode\":\"MCQ\",\"selectedOptionId\":\"B\",\"expectedResponseVersion\":0,\"clientMutationId\":\"" + UUID.randomUUID() + "\"}";
        mvc.perform(put("/api/v1/attempts/{id}/responses/1", attemptId).header("Idempotency-Key", "missing").contentType("application/json").content(body)).andExpect(status().isUnauthorized()).andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
        mvc.perform(put("/api/v1/attempts/{id}/responses/1", attemptId).header("Authorization", "Bearer " + token(owner, ISSUER, AUDIENCE, Instant.now().minusSeconds(1), KEY_PAIR)).header("Idempotency-Key", "expired").contentType("application/json").content(body)).andExpect(status().isUnauthorized());
        mvc.perform(put("/api/v1/attempts/{id}/responses/1", attemptId).header("Authorization", "Bearer " + token(other, ISSUER, AUDIENCE, Instant.now().plusSeconds(60), KEY_PAIR)).header("Idempotency-Key", "other").contentType("application/json").content(body)).andExpect(status().isForbidden());
        mvc.perform(put("/api/v1/attempts/{id}/responses/1", attemptId).header("Authorization", "Bearer " + token(owner, ISSUER, AUDIENCE, Instant.now().plusSeconds(60), KEY_PAIR)).header("Idempotency-Key", "owner").contentType("application/json").content(body)).andExpect(status().isOk()).andExpect(jsonPath("$.selectedOptionId").value("B"));
    }
    @Test void codingSnapshotIsWorkloadOnlyAndReturnsOnlyFrozenCodingData() throws Exception {
        UUID attempt=UUID.randomUUID(), owner=UUID.randomUUID(), assessment=UUID.randomUUID(), assessmentVersion=UUID.randomUUID(), question=UUID.randomUUID(), questionVersion=UUID.randomUUID();
        jdbc.update("insert into assessment.attempts (id, assessment_id, assessment_version_id, assessment_version_number, candidate_user_id, status, started_at, submitted_at, deadline_at, version, created_at, updated_at) values (?, ?, ?, 1, ?, 'SUBMITTED', now(), now(), null, 0, now(), now())",attempt,assessment,assessmentVersion,owner);
        jdbc.update("insert into assessment.attempt_items (attempt_id, global_position, challenge_id, challenge_version_id, challenge_position, question_id, question_version_id, question_position, question_type_code) values (?, 1, ?, ?, 1, ?, ?, 1, 'CODING')",attempt,UUID.randomUUID(),UUID.randomUUID(),question,questionVersion);
        jdbc.update("insert into assessment.submitted_coding_response_snapshots (attempt_id,global_position,question_id,question_version_id,response_state,programming_language,source_code,response_version,source_fingerprint,submitted_at) values (?,1,?,?,'ANSWERED','JAVA','class Main {}',1,?,now())",attempt,question,questionVersion,"a".repeat(64));
        String path="/internal/v1/coding-snapshots/attempts/"+attempt+"/items/1";
        mvc.perform(get(path)).andExpect(status().isUnauthorized());
        mvc.perform(get(path).header("Authorization","Bearer "+token(owner,ISSUER,AUDIENCE,Instant.now().plusSeconds(60),KEY_PAIR))).andExpect(status().isUnauthorized());
        mvc.perform(get(path).header("X-MockArena-Workload-Token","assessment-test-workload-token")).andExpect(status().isOk()).andExpect(jsonPath("$.sourceCode").value("class Main {} ".trim())).andExpect(jsonPath("$.hiddenTests").doesNotExist());
    }
    private void assertUnauthorized(String jwt, UUID versionId) throws Exception { mvc.perform(post("/api/v1/assessments").header("Authorization", "Bearer " + jwt).contentType("application/json").content(request(versionId, UUID.randomUUID()))).andExpect(status().isUnauthorized()).andExpect(jsonPath("$.code").value("UNAUTHENTICATED")); }
    private static String request(UUID versionId, UUID untrustedCreator) { return "{\"createdByUserId\":\"" + untrustedCreator + "\",\"visibility\":\"PRIVATE\",\"content\":{\"title\":\"Authenticated assessment\",\"assessmentTypeCode\":\"STANDARD\",\"timingPolicy\":{\"policyCode\":\"UNTIMED\",\"parameters\":{}},\"attemptPolicy\":{\"policyCode\":\"MAX_ATTEMPTS\",\"parameters\":{\"maxAttempts\":1}},\"resultReleasePolicy\":{\"policyCode\":\"IMMEDIATE\",\"parameters\":{}},\"challengeVersionIds\":[\"" + versionId + "\"]}}"; }
    private static String token(UUID subject, String issuer, String audience, Instant expiry, KeyPair signingKey) throws Exception { JWTClaimsSet claims = new JWTClaimsSet.Builder().subject(subject.toString()).issuer(issuer).audience(List.of(audience)).issueTime(Date.from(Instant.now())).expirationTime(Date.from(expiry)).jwtID(UUID.randomUUID().toString()).claim("roles", List.of("USER")).build(); SignedJWT signed = new SignedJWT(new JWSHeader(JWSAlgorithm.RS256), claims); signed.sign(new RSASSASigner((RSAPrivateKey) signingKey.getPrivate())); return signed.serialize(); }
    private static KeyPair keyPair() { try { KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA"); generator.initialize(2048); return generator.generateKeyPair(); } catch (Exception exception) { throw new IllegalStateException(exception); } }
    private static Path publicKey() { try { Path path = Files.createTempFile("mockarena-assessment-public-", ".pem"); Files.writeString(path, "-----BEGIN PUBLIC KEY-----\n" + Base64.getMimeEncoder(64, new byte[]{'\n'}).encodeToString(KEY_PAIR.getPublic().getEncoded()) + "\n-----END PUBLIC KEY-----\n"); path.toFile().deleteOnExit(); return path; } catch (Exception exception) { throw new IllegalStateException(exception); } }
}
