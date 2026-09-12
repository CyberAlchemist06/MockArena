package com.mockarena.question.security;
import org.springframework.security.core.context.SecurityContextHolder; import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import java.util.UUID;
@Component public class CurrentAuthenticatedUser { private final boolean enabled; public CurrentAuthenticatedUser(@Value("${question.security.enabled:true}") boolean enabled){this.enabled=enabled;} public UUID userId(){Object auth=SecurityContextHolder.getContext().getAuthentication();if(auth instanceof JwtAuthenticationToken jwt)return UUID.fromString(jwt.getToken().getSubject());if(!enabled)return new UUID(0,0);throw new IllegalStateException("Authenticated user is required");} }
