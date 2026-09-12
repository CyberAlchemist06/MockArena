package com.mockarena.assessment.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import java.util.UUID;

@Component public class CurrentAuthenticatedUser {
    private final boolean securityEnabled;
    public CurrentAuthenticatedUser(@Value("${assessment.security.enabled:true}") boolean securityEnabled) { this.securityEnabled = securityEnabled; }
    public UUID userId() { Object authentication = SecurityContextHolder.getContext().getAuthentication(); if (authentication instanceof JwtAuthenticationToken jwt) return UUID.fromString(jwt.getToken().getSubject()); if (!securityEnabled) return new UUID(0, 0); throw new IllegalStateException("Authenticated user is required"); }
}
