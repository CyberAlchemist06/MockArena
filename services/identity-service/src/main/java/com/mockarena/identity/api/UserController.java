package com.mockarena.identity.api;
import com.mockarena.identity.application.AuthenticationService; import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken; import org.springframework.web.bind.annotation.*; import java.util.*;
@RestController @RequestMapping("/api/v1/users") public class UserController {private final AuthenticationService users;public UserController(AuthenticationService users){this.users=users;}
 @GetMapping("/me") public IdentityDtos.UserResponse me(JwtAuthenticationToken token){return users.me(UUID.fromString(token.getToken().getSubject()));}}
