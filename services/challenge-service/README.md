# Challenge Service

Challenge Service owns challenge composition and its PostgreSQL `challenge` schema. This first slice creates a Challenge with one draft, rule-based ChallengeVersion. It calls Question Service's metadata-only catalog API and persists only external `questionId` and `questionVersionId` references.

## Authentication

External Challenge APIs require a MockArena Identity RS256 bearer token. Challenge Service verifies the token signature, issuer, audience, and expiry locally using the Identity public key; it does not access Identity's database. On creation, `created_by_user_id` is derived solely from the authenticated token `sub`. Client-supplied creator fields are not part of the write contract and cannot override it.

Configure the public key outside source control before starting the service:

```powershell
$env:MOCKARENA_JWT_PUBLIC_KEY_PATH = "D:\MockArena\secrets\identity-jwt-public.pem"
$env:MOCKARENA_JWT_ISSUER = "mockarena-identity"
$env:MOCKARENA_JWT_AUDIENCE = "mockarena-api"
```

Health endpoints remain public. The internal resolver remains a separate service boundary until workload authentication is introduced. Challenge Service does not forward an end-user JWT when it calls Question Service.

Run tests with `./mvnw.cmd test`. Integration tests require Docker for PostgreSQL Testcontainers.
