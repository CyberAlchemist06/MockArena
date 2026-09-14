package com.mockarena.assessment.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mockarena.assessment.security.WorkloadTokenFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.*;
import org.springframework.http.*;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.core.*;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import java.nio.file.*;
import java.security.KeyFactory;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.*;

@Configuration public class AssessmentSecurityConfiguration {
    @Bean @ConditionalOnProperty(prefix = "assessment.security", name = "enabled", havingValue = "true", matchIfMissing = true)
    RSAPublicKey assessmentJwtPublicKey(@Value("${assessment.security.jwt.public-key-path}") String path) throws Exception { if (path == null || path.isBlank()) throw new IllegalStateException("MockArena JWT public key path is required"); String pem = Files.readString(Path.of(path)).replace("-----BEGIN PUBLIC KEY-----", "").replace("-----END PUBLIC KEY-----", "").replaceAll("\\s", ""); return (RSAPublicKey) KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(Base64.getDecoder().decode(pem))); }
    @Bean @ConditionalOnProperty(prefix = "assessment.security", name = "enabled", havingValue = "true", matchIfMissing = true)
    JwtDecoder assessmentJwtDecoder(RSAPublicKey key, @Value("${assessment.security.jwt.issuer}") String issuer, @Value("${assessment.security.jwt.audience}") String audience) { NimbusJwtDecoder decoder = NimbusJwtDecoder.withPublicKey(key).build(); OAuth2TokenValidator<Jwt> audienceValidator = jwt -> jwt.getAudience().contains(audience) ? OAuth2TokenValidatorResult.success() : OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token")); decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(JwtValidators.createDefaultWithIssuer(issuer), audienceValidator)); return decoder; }
    @Bean @ConditionalOnProperty(prefix = "assessment.security", name = "enabled", havingValue = "true", matchIfMissing = true)
    SecurityFilterChain secured(HttpSecurity http, JwtDecoder assessmentJwtDecoder, ObjectMapper json, @Value("${assessment.workload-auth.snapshot.token:}") String workloadToken) throws Exception { if(workloadToken==null||workloadToken.isBlank()) throw new IllegalStateException("Assessment snapshot workload token is required when security is enabled"); return http.csrf(csrf -> csrf.disable()).addFilterBefore(new WorkloadTokenFilter(workloadToken,"/internal/v1/coding-snapshots/",json), BearerTokenAuthenticationFilter.class).authorizeHttpRequests(authorize -> authorize.requestMatchers("/actuator/health/**", "/internal/**", "/api/v1/public/assessments", "/api/v1/public/assessments/**").permitAll().anyRequest().authenticated()).oauth2ResourceServer(resource -> resource.jwt(jwt -> jwt.decoder(assessmentJwtDecoder)).authenticationEntryPoint((request, response, exception) -> error(response, json))).build(); }
    @Bean @ConditionalOnProperty(prefix = "assessment.security", name = "enabled", havingValue = "false")
    SecurityFilterChain unsecured(HttpSecurity http) throws Exception { return http.csrf(csrf -> csrf.disable()).authorizeHttpRequests(authorize -> authorize.anyRequest().permitAll()).build(); }
    private static void error(jakarta.servlet.http.HttpServletResponse response, ObjectMapper json) throws java.io.IOException { response.setStatus(HttpStatus.UNAUTHORIZED.value()); response.setContentType(MediaType.APPLICATION_JSON_VALUE); json.writeValue(response.getOutputStream(), Map.of("code", "UNAUTHENTICATED", "message", "Authentication is required")); }
}
