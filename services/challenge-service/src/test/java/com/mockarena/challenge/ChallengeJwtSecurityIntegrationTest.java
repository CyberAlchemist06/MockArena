package com.mockarena.challenge;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mockarena.challenge.domain.RuleBasedSelection;
import com.mockarena.challenge.question.QuestionCatalogClient;
import com.mockarena.challenge.question.QuestionCatalogEntry;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class ChallengeJwtSecurityIntegrationTest {
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
        registry.add("challenge.security.enabled", () -> true);
        registry.add("challenge.security.jwt.public-key-path", () -> PUBLIC_KEY_PATH.toString());
        registry.add("challenge.security.jwt.issuer", () -> ISSUER);
        registry.add("challenge.security.jwt.audience", () -> AUDIENCE);
    }

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired JdbcTemplate jdbc;
    @MockitoBean QuestionCatalogClient catalog;

    @Test
    void validJwtCreatesChallengeForSubjectAndIgnoresSuppliedCreator() throws Exception {
        UUID authenticatedUserId = UUID.randomUUID();
        UUID maliciousCreatorId = UUID.randomUUID();
        when(catalog.resolve(any(RuleBasedSelection.class))).thenReturn(List.of(new QuestionCatalogEntry(UUID.randomUUID(), UUID.randomUUID())));

        String response = mvc.perform(post("/api/v1/challenges")
                .header("Authorization", "Bearer " + token(authenticatedUserId, ISSUER, AUDIENCE, Instant.now().plusSeconds(60), KEY_PAIR))
                .contentType(APPLICATION_JSON).content(createRequest(maliciousCreatorId)))
            .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();

        JsonNode body = objectMapper.readTree(response);
        UUID challengeId = UUID.fromString(body.required("challengeId").asText());
        UUID persistedCreator = jdbc.queryForObject("select created_by_user_id from challenge.challenges where id = ?", UUID.class, challengeId);
        assertThat(persistedCreator).isEqualTo(authenticatedUserId);
        assertThat(persistedCreator).isNotEqualTo(maliciousCreatorId);
    }

    @Test
    void missingTokenIsRejectedWhileHealthRemainsPublic() throws Exception {
        mvc.perform(post("/api/v1/challenges").contentType(APPLICATION_JSON).content(createRequest(UUID.randomUUID())))
            .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
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

    private void assertUnauthenticated(String jwt) throws Exception {
        mvc.perform(post("/api/v1/challenges").header("Authorization", "Bearer " + jwt)
                .contentType(APPLICATION_JSON).content(createRequest(UUID.randomUUID())))
            .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
    }

    private static String createRequest(UUID untrustedCreatorId) {
        return """
            {"createdByUserId":"%s","title":"Authenticated challenge","visibility":"PRIVATE","selection":{"taxonomyAll":[{"scheme":"topic","code":"arrays"}],"questionTypeCodes":["CODING"],"difficultyProfiles":[{"scheme":"mockarena-v1","code":"EASY"}],"contentLocales":["en"],"programmingLanguages":["JAVA"],"requestedQuestionCount":1}}
            """.formatted(untrustedCreatorId);
    }

    private static String token(UUID subject, String issuer, String audience, Instant expiry, KeyPair signingKey) throws Exception {
        JWTClaimsSet claims = new JWTClaimsSet.Builder().subject(subject.toString()).issuer(issuer).audience(List.of(audience))
            .issueTime(Date.from(Instant.now())).expirationTime(Date.from(expiry)).jwtID(UUID.randomUUID().toString()).claim("roles", List.of("USER")).build();
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
            Path path = Files.createTempFile("mockarena-challenge-public-", ".pem");
            String pem = "-----BEGIN PUBLIC KEY-----\n" + java.util.Base64.getMimeEncoder(64, new byte[]{'\n'}).encodeToString(KEY_PAIR.getPublic().getEncoded()) + "\n-----END PUBLIC KEY-----\n";
            Files.writeString(path, pem);
            path.toFile().deleteOnExit();
            return path;
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to write test public key", exception);
        }
    }
}
