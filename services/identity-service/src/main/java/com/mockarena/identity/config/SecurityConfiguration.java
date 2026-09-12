package com.mockarena.identity.config;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.jwk.*;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.proc.SecurityContext;
import org.springframework.context.annotation.*; import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.core.io.*;
import org.springframework.http.*;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.*;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.oauth2.server.resource.authentication.*;
import org.springframework.security.web.SecurityFilterChain;
import java.nio.file.*; import java.security.*; import java.security.interfaces.RSAPrivateKey; import java.security.interfaces.RSAPublicKey; import java.security.spec.*; import java.time.*; import java.util.*;

@Configuration @EnableConfigurationProperties(JwtProperties.class)
public class SecurityConfiguration {
 @Bean PasswordEncoder passwordEncoder(){ return Argon2PasswordEncoder.defaultsForSpringSecurity_v5_8(); }
 @Bean RSAPrivateKey privateKey(JwtProperties p)throws Exception{return (RSAPrivateKey)KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(readPem(p.privateKeyPath(),"PRIVATE KEY")));}
 @Bean RSAPublicKey publicKey(JwtProperties p)throws Exception{return (RSAPublicKey)KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(readPem(p.publicKeyPath(),"PUBLIC KEY")));}
 @Bean JwtEncoder jwtEncoder(RSAPublicKey pub,RSAPrivateKey priv){return new NimbusJwtEncoder(new ImmutableJWKSet<SecurityContext>(new JWKSet(new RSAKey.Builder(pub).privateKey(priv).keyID("identity-v1").build())));}
 @Bean JwtDecoder jwtDecoder(RSAPublicKey pub,JwtProperties p){ NimbusJwtDecoder decoder=NimbusJwtDecoder.withPublicKey(pub).build(); OAuth2TokenValidator<Jwt> audience=jwt->{boolean ok=jwt.getAudience().contains(p.audience());return ok?OAuth2TokenValidatorResult.success():OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token","Invalid audience",null));}; decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(JwtValidators.createDefaultWithIssuer(p.issuer()),audience));return decoder; }
 @Bean SecurityFilterChain security(HttpSecurity http,ObjectMapper json)throws Exception{
  http.csrf(csrf->csrf.disable()).authorizeHttpRequests(a->a.requestMatchers("/api/v1/auth/**","/actuator/health/**").permitAll().anyRequest().authenticated())
   .oauth2ResourceServer(o->o.jwt(j->j.jwtAuthenticationConverter(converter())).authenticationEntryPoint((r,s,e)->error(s,json,HttpStatus.UNAUTHORIZED,"UNAUTHENTICATED","Authentication is required")))
   .exceptionHandling(e->e.accessDeniedHandler((r,s,x)->error(s,json,HttpStatus.FORBIDDEN,"FORBIDDEN","Insufficient role")));
  return http.build(); }
 private static JwtAuthenticationConverter converter(){JwtGrantedAuthoritiesConverter roles=new JwtGrantedAuthoritiesConverter();roles.setAuthoritiesClaimName("roles");roles.setAuthorityPrefix("ROLE_");JwtAuthenticationConverter c=new JwtAuthenticationConverter();c.setJwtGrantedAuthoritiesConverter(roles);return c;}
 private static void error(jakarta.servlet.http.HttpServletResponse s,ObjectMapper json,HttpStatus status,String code,String message)throws java.io.IOException{s.setStatus(status.value());s.setContentType(MediaType.APPLICATION_JSON_VALUE);json.writeValue(s.getOutputStream(),Map.of("code",code,"message",message));}
 private static byte[] readPem(String path,String label)throws Exception{if(path==null||path.isBlank())throw new IllegalStateException("JWT key path is required");String pem=Files.readString(Path.of(path));String value=pem.replace("-----BEGIN "+label+"-----","").replace("-----END "+label+"-----","").replaceAll("\\s","");return Base64.getDecoder().decode(value);}
}
