package com.mockarena.challenge.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.web.SecurityFilterChain;

import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyFactory;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.Map;

@Configuration
public class ChallengeSecurityConfiguration {
    @Bean
    @ConditionalOnProperty(prefix = "challenge.security", name = "enabled", havingValue = "true", matchIfMissing = true)
    RSAPublicKey challengeJwtPublicKey(@Value("${challenge.security.jwt.public-key-path}") String path) throws Exception {
        if (path == null || path.isBlank()) throw new IllegalStateException("MockArena JWT public key path is required");
        String pem = Files.readString(Path.of(path)).replace("-----BEGIN PUBLIC KEY-----", "").replace("-----END PUBLIC KEY-----", "").replaceAll("\\s", "");
        return (RSAPublicKey) KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(Base64.getDecoder().decode(pem)));
    }

    @Bean
    @ConditionalOnProperty(prefix = "challenge.security", name = "enabled", havingValue = "true", matchIfMissing = true)
    JwtDecoder challengeJwtDecoder(RSAPublicKey publicKey, @Value("${challenge.security.jwt.issuer}") String issuer, @Value("${challenge.security.jwt.audience}") String audience) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withPublicKey(publicKey).build();
        OAuth2TokenValidator<Jwt> audienceValidator = jwt -> jwt.getAudience().contains(audience)
            ? OAuth2TokenValidatorResult.success()
            : OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token", "Invalid audience", null));
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(JwtValidators.createDefaultWithIssuer(issuer), audienceValidator));
        return decoder;
    }

    @Bean
    @ConditionalOnProperty(prefix = "challenge.security", name = "enabled", havingValue = "true", matchIfMissing = true)
    SecurityFilterChain secured(HttpSecurity http, JwtDecoder challengeJwtDecoder, ObjectMapper objectMapper) throws Exception {
        return http.csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(authorize -> authorize.requestMatchers("/actuator/health/**", "/internal/**").permitAll().anyRequest().authenticated())
            .oauth2ResourceServer(resourceServer -> resourceServer.jwt(jwt -> jwt.decoder(challengeJwtDecoder))
                .authenticationEntryPoint((request, response, exception) -> error(response, objectMapper, HttpStatus.UNAUTHORIZED, "UNAUTHENTICATED")))
            .build();
    }

    @Bean
    @ConditionalOnProperty(prefix = "challenge.security", name = "enabled", havingValue = "false")
    SecurityFilterChain unsecured(HttpSecurity http) throws Exception {
        return http.csrf(csrf -> csrf.disable()).authorizeHttpRequests(authorize -> authorize.anyRequest().permitAll()).build();
    }

    private static void error(jakarta.servlet.http.HttpServletResponse response, ObjectMapper objectMapper, HttpStatus status, String code) throws java.io.IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(), Map.of("code", code, "message", "Authentication is required"));
    }
}
