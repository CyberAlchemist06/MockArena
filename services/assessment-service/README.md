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
# Attempt submission

`POST /api/v1/attempts/{attemptId}/submit` requires the owner JWT and an `Idempotency-Key`. It changes only an in-progress local Attempt to `SUBMITTED`; it does not evaluate, score, or retrieve Question/Challenge data. Submitted Attempts reject response writes.

## MCQ evaluation and candidate results

After a successful local submit commit, Assessment Service publishes a local event and performs a best-effort MCQ evaluation using Question Service's protected historical evaluation projection. Evaluation failures do not invalidate the successful submission. The V1 event is synchronous-after-commit, so its work can add submit response latency; it is not a durable job mechanism.

`POST /internal/v1/attempts/{attemptId}/evaluate` is an internal operational retry path for a `SUBMITTED` Attempt. It returns only the attempt ID and aggregate evaluation status and is not exposed through the browser/BFF.

`GET /api/v1/attempts/{attemptId}/result` requires the owner JWT and is read-only. A submitted Attempt with no durable result returns `PENDING`. MCQ-only results can be `EVALUATED`; mixed MCQ/CODING results are `PARTIALLY_EVALUATED`, keep coding items pending, and deliberately omit a final score. Results never return correct answers, explanations, Question scoring-policy data, hidden tests, or candidate source code.

## Coding evaluation preparation

V9 snapshots each coding AttemptItem in the successful submit transaction. An answered snapshot stores the exact language, source, response version, SHA-256 source fingerprint, and submitted time; an unanswered coding item stores an explicit `UNANSWERED` snapshot. The same transaction creates one opaque `CODING_EVALUATION_REQUESTED` outbox record per coding item. Neither snapshots nor outbox records contain hidden tests or Question execution/scoring specifications, and the outbox does not copy source code. There is no relay, Evaluation Service, runner, or candidate-code execution in this phase.

Result visibility is derived from the immutable AssessmentVersion release policy. `IMMEDIATE` returns persisted result facts; `SCHEDULED` stays opaque before its configured `releaseAt`; `MANUAL` remains unavailable until an explicit future release capability exists. The V8 `released_at` column is intentionally not updated by a read.
