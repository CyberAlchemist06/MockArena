package com.mockarena.identity.application;
import com.mockarena.identity.api.IdentityDtos.*; import com.mockarena.identity.domain.*;
import org.springframework.security.crypto.password.PasswordEncoder; import org.springframework.stereotype.Service; import org.springframework.transaction.annotation.Transactional;
import java.time.*; import java.util.*;
@Service public class AuthenticationService { private final IdentityUserRepository users; private final PasswordEncoder passwords; private final TokenService tokens;
 public AuthenticationService(IdentityUserRepository users,PasswordEncoder passwords,TokenService tokens){this.users=users;this.passwords=passwords;this.tokens=tokens;}
 @Transactional public UserResponse register(RegisterRequest r){String email=canonical(r.email());if(users.findByEmail(email).isPresent())throw new DuplicateEmailException();IdentityUser user=users.save(new IdentityUser(UUID.randomUUID(),email,r.displayName().trim(),passwords.encode(r.password()),Instant.now()));return response(user);}
 @Transactional(readOnly=true) public LoginResponse login(LoginRequest r){IdentityUser user=users.findByEmail(canonical(r.email())).orElseThrow(InvalidCredentialsException::new);if(user.status()!=UserStatus.ACTIVE||!passwords.matches(r.password(),user.passwordHash()))throw new InvalidCredentialsException();return new LoginResponse(tokens.issue(user),"Bearer",TokenService.EXPIRY_SECONDS);}
 @Transactional(readOnly=true) public UserResponse me(UUID id){return response(users.findById(id).orElseThrow(()->new NoSuchElementException("User not found")));}
 private static String canonical(String email){return email.trim().toLowerCase(Locale.ROOT);} private static UserResponse response(IdentityUser u){return new UserResponse(u.id(),u.email(),u.displayName(),u.roles().stream().map(Enum::name).sorted().toList());}
}
