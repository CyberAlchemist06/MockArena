# ADR 0014: Challenge Service local JWT authentication

- **Status:** Accepted
- **Date:** 2026-09-12

## Decision

Challenge Service validates MockArena Identity's RS256 access tokens locally with a configured X.509 RSA public key. Validation requires a valid signature, issuer, audience, and expiry. External challenge APIs require bearer authentication, while health endpoints remain public.

The JWT `sub` UUID is the sole source for `Challenge.createdByUserId` at creation. Creator fields are not accepted as part of the external write contract. The additive `created_by_user_id` column is nullable only to preserve historical Challenge records created before Identity integration; all new Challenges receive the authenticated subject.

Question catalog resolution remains a service-to-service boundary. Challenge Service does not forward the end-user bearer token to Question Service. Internal APIs retain their current temporary workload-authentication posture and must not treat an end-user JWT as workload identity.

## Consequences

- Challenge Service has no Identity database dependency and no private signing key.
- Identity key rotation requires deployment configuration changes or a future JWKS/workload-authentication design.
- Authentication establishes provenance. Per-user authorization and organization access rules remain future work.
