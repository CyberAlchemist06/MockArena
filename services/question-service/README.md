# Question Service

The Question Service owns versioned assessment content in the PostgreSQL `question` schema. Its APIs are under `/api/v1/questions`.

Hidden tests are persisted as JSONB for V1, but are deliberately excluded from every HTTP response and entity `toString`.

## Authentication

External Question APIs require a MockArena Identity RS256 bearer token. The service verifies its signature, issuer, audience, and expiry locally; it does not access the Identity Service database. `POST /api/v1/questions` derives the owner from the token `sub` claim. `ownerUserId` is not part of the write contract and cannot control ownership if supplied by an older client.

Configure the Identity public key outside source control before starting the service:

```powershell
$env:MOCKARENA_JWT_PUBLIC_KEY_PATH = "D:\MockArena\secrets\identity-jwt-public.pem"
$env:MOCKARENA_JWT_ISSUER = "mockarena-identity"
$env:MOCKARENA_JWT_AUDIENCE = "mockarena-api"
```

The `/actuator/health` endpoints remain public. The `/internal/**` catalog APIs intentionally remain a service boundary without end-user JWT authentication until workload authentication is introduced; they never expose question content or protected answer data.

Challenge Service can query the metadata-only internal catalog at `POST /internal/v1/question-versions/resolve`. It returns only currently reusable published QuestionVersions and never returns question prompts, constraints, examples, tests, scoring, or execution limits. Authentication for this internal route is intentionally deferred in V1.

Published question versions are immutable in both application rules and a PostgreSQL trigger. Mutating version operations require the client’s JPA version value; PostgreSQL's unique `(question_id, version_number)` constraint provides an additional concurrency safeguard. No distributed locking is used.

Health is at `/actuator/health`; Kubernetes-compatible probe paths are `/actuator/health/liveness` and `/actuator/health/readiness`.

Run tests from this directory with `./mvnw.cmd test`. The first run downloads Maven if it is not already cached. Integration tests use Testcontainers and require a running Docker daemon.

## Development seed data

To seed the 14 development questions (12 CODING and 2 MCQ), run with the `dev` profile and an explicit opt-in flag:

```powershell
$env:SPRING_PROFILES_ACTIVE = "dev"
$env:QUESTION_SEED_DEVELOPMENT_DATA = "true"
.\mvnw.cmd spring-boot:run
```

The seed uses the normal Question Service create/publish lifecycle and is idempotent by its dedicated development owner and question titles. It is inactive outside the `dev` profile and without the flag.
