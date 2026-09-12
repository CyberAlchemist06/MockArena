package com.mockarena.identity.application;
import com.mockarena.identity.config.JwtProperties; import com.mockarena.identity.domain.IdentityUser;
import org.springframework.security.oauth2.jwt.*; import org.springframework.stereotype.Service;
import java.time.*; import java.util.*;
@Service public class TokenService { public static final long EXPIRY_SECONDS=900; private final JwtEncoder encoder; private final JwtProperties props;
 public TokenService(JwtEncoder encoder,JwtProperties props){this.encoder=encoder;this.props=props;}
 public String issue(IdentityUser user){Instant now=Instant.now();JwtClaimsSet claims=JwtClaimsSet.builder().issuer(props.issuer()).subject(user.id().toString()).audience(List.of(props.audience())).issuedAt(now).expiresAt(now.plusSeconds(EXPIRY_SECONDS)).id(UUID.randomUUID().toString()).claim("roles",user.roles().stream().map(Enum::name).sorted().toList()).build();return encoder.encode(JwtEncoderParameters.from(claims)).getTokenValue();}
}
