package com.mockarena.identity.api;
import jakarta.validation.constraints.*;
import java.util.*;
public final class IdentityDtos { private IdentityDtos(){}
 public record RegisterRequest(@NotBlank @Email @Size(max=320) String email,@NotBlank @Size(max=120) String displayName,@NotBlank @Size(min=12,max=200) String password){@Override public String toString(){return "RegisterRequest[email="+email+"]";}}
 public record LoginRequest(@NotBlank @Email @Size(max=320) String email,@NotBlank @Size(max=200) String password){@Override public String toString(){return "LoginRequest[email="+email+"]";}}
 public record UserResponse(UUID userId,String email,String displayName,List<String> roles){}
 public record LoginResponse(String accessToken,String tokenType,long expiresInSeconds){}
}
