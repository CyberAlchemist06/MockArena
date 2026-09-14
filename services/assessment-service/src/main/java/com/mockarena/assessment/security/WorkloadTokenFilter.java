package com.mockarena.assessment.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.*; import jakarta.servlet.http.*;
import org.springframework.http.MediaType; import org.springframework.web.filter.OncePerRequestFilter;
import java.io.*; import java.nio.charset.StandardCharsets; import java.security.MessageDigest; import java.util.Map;

/** Guards a single sensitive workload route; candidate JWTs are not service identity. */
public final class WorkloadTokenFilter extends OncePerRequestFilter {
 private final byte[] expected; private final ObjectMapper json; private final String path;
 public WorkloadTokenFilter(String token,String path,ObjectMapper json){this.expected=token.getBytes(StandardCharsets.UTF_8);this.path=path;this.json=json;}
 @Override protected boolean shouldNotFilter(HttpServletRequest request){return !request.getRequestURI().startsWith(path);}
 @Override protected void doFilterInternal(HttpServletRequest request,HttpServletResponse response,FilterChain chain)throws ServletException,IOException {String actual=request.getHeader("X-MockArena-Workload-Token");if(actual==null||!MessageDigest.isEqual(expected,actual.getBytes(StandardCharsets.UTF_8))){response.setStatus(401);response.setContentType(MediaType.APPLICATION_JSON_VALUE);json.writeValue(response.getOutputStream(),Map.of("code","UNAUTHENTICATED","message","Workload authentication is required"));return;}chain.doFilter(request,response);}
}
