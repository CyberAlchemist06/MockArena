package com.mockarena.question;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
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

import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class QuestionJwtSecurityIntegrationTest {
    private static final String ISSUER = "mockarena-identity";
    private static final String AUDIENCE = "mockarena-api";
    private static final KeyPair KEY_PAIR = keyPair();
    private static final Path PUBLIC_KEY_PATH = publicKeyFile();

    @Container static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("question.security.enabled", () -> true);
        registry.add("question.security.jwt.public-key-path", () -> PUBLIC_KEY_PATH.toString());
        registry.add("question.security.jwt.issuer", () -> ISSUER);
        registry.add("question.security.jwt.audience", () -> AUDIENCE);
        registry.add("question.workload-auth.coding-evaluation.token", () -> "test-workload-token");
    }

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired JdbcTemplate jdbc;

    @Test
    void validJwtCreatesQuestionForSubjectAndIgnoresSuppliedOwner() throws Exception {
        UUID authenticatedUserId = UUID.randomUUID();
        UUID maliciousOwnerId = UUID.randomUUID();

        String response = mvc.perform(post("/api/v1/questions")
                .header("Authorization", "Bearer " + token(authenticatedUserId, ISSUER, AUDIENCE, Instant.now().plusSeconds(60), KEY_PAIR))
                .contentType(MediaType.APPLICATION_JSON)
                .content(createRequest(maliciousOwnerId)))
            .andExpect(status().isCreated())
            .andReturn().getResponse().getContentAsString();

        JsonNode body = objectMapper.readTree(response);
        UUID questionId = UUID.fromString(body.required("questionId").asText());
        UUID persistedOwner = jdbc.queryForObject("select owner_user_id from question.questions where id = ?", UUID.class, questionId);
        assertThat(persistedOwner).isEqualTo(authenticatedUserId);
        assertThat(persistedOwner).isNotEqualTo(maliciousOwnerId);
    }

    @Test
    void missingTokenIsRejectedWhileHealthRemainsPublic() throws Exception {
        mvc.perform(post("/api/v1/questions").contentType(MediaType.APPLICATION_JSON).content(createRequest(UUID.randomUUID())))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
        mvc.perform(get("/actuator/health")).andExpect(status().isOk());
    }

    @Test
    void invalidJwtClaimsAndSignatureAreRejected() throws Exception {
        UUID userId = UUID.randomUUID();
        assertUnauthenticated(token(userId, ISSUER, AUDIENCE, Instant.now().minusSeconds(60), KEY_PAIR));
        assertUnauthenticated(token(userId, "other-issuer", AUDIENCE, Instant.now().plusSeconds(60), KEY_PAIR));
        assertUnauthenticated(token(userId, ISSUER, "other-audience", Instant.now().plusSeconds(60), KEY_PAIR));
        assertUnauthenticated(token(userId, ISSUER, AUDIENCE, Instant.now().plusSeconds(60), keyPair()));
    }

    @Test
    void codingEvaluationEndpointRequiresWorkloadIdentityNotCandidateJwt() throws Exception {
        mvc.perform(post("/internal/v1/question-versions/coding-evaluation-data").contentType(MediaType.APPLICATION_JSON)
                .content("{\"questionVersionIds\":[\"" + UUID.randomUUID() + "\"]}"))
            .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
        mvc.perform(post("/internal/v1/question-versions/coding-evaluation-data")
                .header("Authorization", "Bearer " + token(UUID.randomUUID(), ISSUER, AUDIENCE, Instant.now().plusSeconds(60), KEY_PAIR))
                .contentType(MediaType.APPLICATION_JSON).content("{\"questionVersionIds\":[\"" + UUID.randomUUID() + "\"]}"))
            .andExpect(status().isUnauthorized());
        mvc.perform(post("/internal/v1/question-versions/coding-evaluation-data").header("X-MockArena-Workload-Token", "test-workload-token")
                .contentType(MediaType.APPLICATION_JSON).content("{\"questionVersionIds\":[\"" + UUID.randomUUID() + "\"]}"))
            .andExpect(status().isNotFound());
    }

    private void assertUnauthenticated(String jwt) throws Exception {
        mvc.perform(post("/api/v1/questions")
                .header("Authorization", "Bearer " + jwt)
                .contentType(MediaType.APPLICATION_JSON)
                .content(createRequest(UUID.randomUUID())))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
    }

    private static String createRequest(UUID untrustedOwnerId) {
        return """
            {"ownerUserId":"%s","content":{"title":"Authenticated question","tags":["arrays"],"difficulty":"EASY","questionType":"CODING","prompt":"Find a pair","constraintsText":"n >= 2","examples":[],"supportedLanguages":["JAVA"],"visibleTests":[],"hiddenTests":[],"scoringRules":{"points":100},"executionLimits":{"timeMs":1000}}}
            """.formatted(untrustedOwnerId);
    }

    private static String token(UUID subject, String issuer, String audience, Instant expiry, KeyPair signingKey) throws Exception {
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
            .subject(subject.toString()).issuer(issuer).audience(List.of(audience))
            .issueTime(Date.from(Instant.now())).expirationTime(Date.from(expiry)).jwtID(UUID.randomUUID().toString())
            .claim("roles", List.of("USER")).build();
        SignedJWT signed = new SignedJWT(new JWSHeader(JWSAlgorithm.RS256), claims);
        signed.sign(new RSASSASigner((RSAPrivateKey) signingKey.getPrivate()));
        return signed.serialize();
    }

    private static KeyPair keyPair() {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            return generator.generateKeyPair();
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to create test signing key", exception);
        }
    }

    private static Path publicKeyFile() {
        try {
            Path path = Files.createTempFile("mockarena-question-public-", ".pem");
            String pem = "-----BEGIN PUBLIC KEY-----\n" + java.util.Base64.getMimeEncoder(64, new byte[]{'\n'}).encodeToString(KEY_PAIR.getPublic().getEncoded()) + "\n-----END PUBLIC KEY-----\n";
            Files.writeString(path, pem);
            path.toFile().deleteOnExit();
            return path;
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to write test public key", exception);
        }
    }
}
