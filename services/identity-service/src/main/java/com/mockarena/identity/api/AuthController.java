package com.mockarena.identity.api;
import com.mockarena.identity.application.AuthenticationService; import jakarta.validation.Valid; import org.springframework.http.*; import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/v1/auth") public class AuthController {private final AuthenticationService auth; public AuthController(AuthenticationService auth){this.auth=auth;}
 @PostMapping("/register") public ResponseEntity<IdentityDtos.UserResponse> register(@Valid @RequestBody IdentityDtos.RegisterRequest request){return ResponseEntity.status(HttpStatus.CREATED).body(auth.register(request));}
 @PostMapping("/login") public IdentityDtos.LoginResponse login(@Valid @RequestBody IdentityDtos.LoginRequest request){return auth.login(request);}}
