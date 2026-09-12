# Question Service

The Question Service owns versioned DSA question content in the PostgreSQL `question` schema. Its APIs are under `/api/v1/questions`.

Hidden tests are persisted as JSONB for V1, but are deliberately excluded from every HTTP response and entity `toString`. `ownerUserId` is provenance metadata only; no authorization decision is implemented until Identity/Security is integrated.

Challenge Service can query the metadata-only internal catalog at `POST /internal/v1/question-versions/resolve`. It returns only currently reusable published QuestionVersions and never returns question prompts, constraints, examples, tests, scoring, or execution limits. Authentication for this internal route is intentionally deferred in V1.

Published question versions are immutable in both application rules and a PostgreSQL trigger. Mutating version operations require the client’s JPA version value; PostgreSQL's unique `(question_id, version_number)` constraint provides an additional concurrency safeguard. No distributed locking is used.

Health is at `/actuator/health`; Kubernetes-compatible probe paths are `/actuator/health/liveness` and `/actuator/health/readiness`.

Run tests from this directory with `./mvnw.cmd test`. The first run downloads Maven if it is not already cached. Integration tests use Testcontainers and require a running Docker daemon.

## Development seed data

To seed the 12 original development questions, run with the `dev` profile and an explicit opt-in flag:

```powershell
$env:SPRING_PROFILES_ACTIVE = "dev"
$env:QUESTION_SEED_DEVELOPMENT_DATA = "true"
.\mvnw.cmd spring-boot:run
```

The seed uses the normal Question Service create/publish lifecycle and is idempotent by its dedicated development owner and question titles. It is inactive outside the `dev` profile and without the flag.
