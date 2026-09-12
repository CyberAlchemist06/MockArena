# Assessment Service

Assessment Service owns Assessments and immutable AssessmentVersions in the PostgreSQL `assessment` schema. It composes only ordered, published ChallengeVersion references through Challenge Service's metadata-only internal resolver; it never accesses another service's database or copies Question content.

External APIs require a MockArena Identity RS256 bearer token. Configure the matching public key outside source control:

```powershell
$env:MOCKARENA_JWT_PUBLIC_KEY_PATH = "D:\MockArena\secrets\identity-jwt-public.pem"
$env:MOCKARENA_JWT_ISSUER = "mockarena-identity"
$env:MOCKARENA_JWT_AUDIENCE = "mockarena-api"
```

The local port is `8083`. Run tests with `./mvnw.cmd clean test`; PostgreSQL integration tests require Docker.

AssessmentVersion timing is immutable when published: optional UTC `availableFrom`/`availableUntil` control future starts, and optional `attemptDurationSeconds` defines the future per-attempt duration. A future Attempt deadline is the earlier of the duration deadline and `availableUntil`.
