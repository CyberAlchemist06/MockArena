# Assessment Service

Assessment Service owns Assessments and immutable AssessmentVersions in the PostgreSQL `assessment` schema. It composes only ordered, published ChallengeVersion references through Challenge Service's metadata-only internal resolver; it never accesses another service's database or copies Question content.

External APIs require a MockArena Identity RS256 bearer token, except the anonymous public catalogue endpoints: `GET /api/v1/public/assessments` and `GET /api/v1/public/assessments/{assessmentId}`. They return only current `PUBLIC`/`PUBLISHED` assessment summaries and never expose manifests, Question data, or candidate data. Configure the matching public key outside source control:

```powershell
$env:MOCKARENA_JWT_PUBLIC_KEY_PATH = "D:\MockArena\secrets\identity-jwt-public.pem"
$env:MOCKARENA_JWT_ISSUER = "mockarena-identity"
$env:MOCKARENA_JWT_AUDIENCE = "mockarena-api"
```

The local port is `8083`. Run tests with `./mvnw.cmd clean test`; PostgreSQL integration tests require Docker.

AssessmentVersion timing is immutable when published: optional UTC `availableFrom`/`availableUntil` control future starts, and optional `attemptDurationSeconds` defines the future per-attempt duration. A future Attempt deadline is the earlier of the duration deadline and `availableUntil`.

## Public catalogue backfill

The publication path creates the safe `assessment.public_assessment_catalogue` projection transactionally for new published AssessmentVersions. To backfill eligible published rows created before the projection existed, start Assessment Service with the explicit maintenance flag after Challenge Service is available:

```powershell
$env:ASSESSMENT_CATALOGUE_BACKFILL_ENABLED = "true"
.\mvnw.cmd spring-boot:run
```

The runner is disabled by default. It finds only current `PUBLIC` + `PUBLISHED` rows missing a projection, uses Challenge Service's internal V2 manifest API to calculate safe counts, and logs each success or failure. It is idempotent: reruns do not replace existing projections. Set the variable back to `false` or remove it after the run.
