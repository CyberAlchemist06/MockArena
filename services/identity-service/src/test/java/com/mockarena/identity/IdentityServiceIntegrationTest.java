package com.mockarena.identity;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.*;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.test.context.*;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.*;
import java.nio.file.*; import java.security.*; import java.time.*; import java.util.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest @AutoConfigureMockMvc @Testcontainers
class IdentityServiceIntegrationTest {
 @Container static final PostgreSQLContainer<?> postgres=new PostgreSQLContainer<>("postgres:16-alpine");
 static final Path privateKey; static final Path publicKey;
 static { try { KeyPair pair=KeyPairGenerator.getInstance("RSA").generateKeyPair(); privateKey=write("PRIVATE KEY",pair.getPrivate().getEncoded()); publicKey=write("PUBLIC KEY",pair.getPublic().getEncoded()); } catch(Exception e){throw new ExceptionInInitializerError(e);} }
 static Path write(String label,byte[] bytes)throws Exception{Path path=Files.createTempFile("identity-",".pem");Files.writeString(path,"-----BEGIN "+label+"-----\n"+Base64.getMimeEncoder(64,new byte[]{'\n'}).encodeToString(bytes)+"\n-----END "+label+"-----\n");return path;}
 @DynamicPropertySource static void properties(DynamicPropertyRegistry r){r.add("spring.datasource.url",postgres::getJdbcUrl);r.add("spring.datasource.username",postgres::getUsername);r.add("spring.datasource.password",postgres::getPassword);r.add("identity.jwt.private-key-path",()->privateKey.toString());r.add("identity.jwt.public-key-path",()->publicKey.toString());}
 @Autowired MockMvc mvc; @Autowired ObjectMapper json; @Autowired JdbcTemplate jdbc; @Autowired PasswordEncoder passwords; @Autowired JwtEncoder encoder;

 @Test void registersNormalizedEmailWithArgon2idHashAndNeverReturnsSensitiveFields() throws Exception {
  String response=mvc.perform(post("/api/v1/auth/register").contentType("application/json").content("{\"email\":\"Ada@Example.COM\",\"displayName\":\"Ada\",\"password\":\"correct-horse-battery\"}"))
   .andExpect(status().isCreated()).andExpect(jsonPath("$.email").value("ada@example.com")).andExpect(jsonPath("$.password").doesNotExist()).andExpect(jsonPath("$.passwordHash").doesNotExist()).andReturn().getResponse().getContentAsString();
  String id=json.readTree(response).get("userId").asText();String hash=jdbc.queryForObject("select password_hash from identity.users where id=?",String.class,UUID.fromString(id));assertThat(hash).startsWith("$argon2id$");assertThat(passwords.matches("correct-horse-battery",hash)).isTrue();assertThat(hash).doesNotContain("correct-horse-battery");
  mvc.perform(post("/api/v1/auth/register").contentType("application/json").content("{\"email\":\"ADA@example.com\",\"displayName\":\"Other\",\"password\":\"correct-horse-battery\"}")).andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("EMAIL_ALREADY_REGISTERED"));
 }
 @Test void loginErrorsAreIndistinguishableAndValidTokenAuthenticatesMe() throws Exception {
  mvc.perform(post("/api/v1/auth/register").contentType("application/json").content("{\"email\":\"user@example.com\",\"displayName\":\"User\",\"password\":\"correct-horse-battery\"}")).andExpect(status().isCreated());
  for(String body:List.of("{\"email\":\"missing@example.com\",\"password\":\"correct-horse-battery\"}","{\"email\":\"user@example.com\",\"password\":\"wrong-password-123\"}"))mvc.perform(post("/api/v1/auth/login").contentType("application/json").content(body)).andExpect(status().isUnauthorized()).andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));
  String login=mvc.perform(post("/api/v1/auth/login").contentType("application/json").content("{\"email\":\"user@example.com\",\"password\":\"correct-horse-battery\"}")).andExpect(status().isOk()).andExpect(jsonPath("$.tokenType").value("Bearer")).andExpect(jsonPath("$.expiresInSeconds").value(900)).andReturn().getResponse().getContentAsString();
  String token=json.readTree(login).get("accessToken").asText();mvc.perform(get("/api/v1/users/me").header("Authorization","Bearer "+token)).andExpect(status().isOk()).andExpect(jsonPath("$.email").value("user@example.com")).andExpect(jsonPath("$.roles[0]").value("USER"));
 }
 @Test void rejectsMissingInvalidExpiredWrongIssuerAndWrongAudienceTokens() throws Exception {
  mvc.perform(get("/api/v1/users/me")).andExpect(status().isUnauthorized()).andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
  mvc.perform(get("/api/v1/users/me").header("Authorization","Bearer invalid.token.value")).andExpect(status().isUnauthorized());
  for(JwtClaimsSet claims:List.of(claims("mockarena-identity","mockarena-api",Instant.now().minusSeconds(100)),claims("wrong-issuer","mockarena-api",Instant.now().plusSeconds(100)),claims("mockarena-identity","wrong-audience",Instant.now().plusSeconds(100)))){String token=encoder.encode(JwtEncoderParameters.from(claims)).getTokenValue();mvc.perform(get("/api/v1/users/me").header("Authorization","Bearer "+token)).andExpect(status().isUnauthorized());}
 }
 private static JwtClaimsSet claims(String issuer,String audience,Instant expiry){return JwtClaimsSet.builder().issuer(issuer).subject(UUID.randomUUID().toString()).audience(List.of(audience)).issuedAt(expiry.minusSeconds(60)).expiresAt(expiry).id(UUID.randomUUID().toString()).claim("roles",List.of("USER")).build();}
}
