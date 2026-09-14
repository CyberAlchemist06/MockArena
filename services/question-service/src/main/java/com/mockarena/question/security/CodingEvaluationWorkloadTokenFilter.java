package com.mockarena.question.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Map;

/** Transitional workload guard for the hidden coding-evaluation endpoint. */
public final class CodingEvaluationWorkloadTokenFilter extends OncePerRequestFilter {
    public static final String HEADER = "X-MockArena-Workload-Token";
    private static final String PATH = "/internal/v1/question-versions/coding-evaluation-data";
    private final byte[] expected;
    private final ObjectMapper json;

    public CodingEvaluationWorkloadTokenFilter(String token, ObjectMapper json) { this.expected = token.getBytes(StandardCharsets.UTF_8); this.json = json; }
    @Override protected boolean shouldNotFilter(HttpServletRequest request) { return !PATH.equals(request.getRequestURI()); }
    @Override protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws ServletException, IOException {
        String presented = request.getHeader(HEADER);
        if (presented == null || !MessageDigest.isEqual(expected, presented.getBytes(StandardCharsets.UTF_8))) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED); response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            json.writeValue(response.getOutputStream(), Map.of("code", "UNAUTHENTICATED", "message", "Authentication is required")); return;
        }
        chain.doFilter(request, response);
    }
}
