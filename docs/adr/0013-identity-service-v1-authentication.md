# ADR 0013: Identity Service V1 authentication

- **Status:** Accepted
- **Date:** 2026-09-12

## Decision

Identity Service owns users, password hashes, `USER`/`ADMIN` roles, and authentication. It stores Argon2id hashes in the `identity` schema and issues 15-minute RS256 JWT access tokens. Tokens contain only subject, roles, issuer, audience, issued/expiry times, and a token ID; they are not persisted.

Signing uses a private PKCS#8 RSA PEM key supplied by environment path. Verification uses the matching public X.509 PEM key. Keys are never generated or stored in the repository. `/api/v1/users/me` is protected by local bearer-token verification; registration and login are public.

## Consequences

- Existing services remain unchanged until they adopt Identity's public-key verification and derive provenance from JWT `sub`.
- V1 has no refresh token, logout, Redis session, password reset, MFA, social login, or organization membership.
- Short token lifetime bounds revocation delay while avoiding a central session dependency.
