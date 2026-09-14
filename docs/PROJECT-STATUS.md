# MockArena Project Status

Checkpoint date: 2026-09-14

## Implemented services

- Question Service (`8080`): generic versioned assessment content.
- Challenge Service (`8081`): generic question selection and immutable challenge manifests.
- Identity Service (`8082`): local user identity and access-token issuance.
- Assessment Service (`8083`): assessments, publication, candidate attempts/content/autosave, and anonymous public catalogue reads.
- Next.js frontend: same-origin BFF, public catalogue, and authenticated candidate flows.

PostgreSQL 16 runs locally in the existing `mockarena-postgres` Docker container. Each service owns its own PostgreSQL schema; services communicate over versioned REST APIs and never read another service's tables.

## Database migrations

- Question Service: V5 `snapshot_generic_metadata_for_historical_question_versions`
- Challenge Service: V5 `add_question_type_to_challenge_version_manifest`
- Identity Service: V1 `identity_schema`
- Assessment Service: V6 `add_public_assessment_catalogue`

All schema evolution uses Flyway. Published QuestionVersions, ChallengeVersions, and AssessmentVersions/manifests are protected as immutable historical records.

## Authentication

Identity Service uses Argon2id password hashing and issues 15-minute RS256 JWT access tokens. The token contains the user UUID in `sub`, roles, issuer, audience, issued/expiry timestamps, and a token ID.

Question, Challenge, and Assessment services validate the Identity public key locally (signature, issuer, audience, and expiry). User-owned writes derive their actor/creator identity only from JWT `sub`; client-supplied owner or creator IDs are not trusted. Internal service APIs remain a separate service-to-service boundary and do not forward browser JWTs.

## Question Service

- Versioned generic assessment content with `MCQ` and `CODING` type handlers.
- Generic taxonomy assignments, content locale, difficulty profiles, programming languages, and scoring-policy envelopes.
- Safe catalog APIs: legacy V1 compatibility and generic V2 resolution.
- Candidate-content V3 API returns exact requested historical `PUBLISHED` or `RETIRED` QuestionVersions while omitting MCQ correct answers/explanations and coding hidden tests, scoring rules, and execution limits.
- Development seed data includes DSA coding and MCQ content.

## Challenge Service

- Generic rule-based selection using taxonomy, question type, difficulty profile, locale, and programming language metadata.
- Deterministic selection, logical-question de-duplication, and ID-only composition.
- Draft publication resolves Question Service metadata into an exact, ordered, immutable published QuestionVersion manifest.
- Published ChallengeVersions can retire without rewriting their historical manifest; retiring the current version clears the current-published pointer but does not archive the Challenge.
- Internal V1 resolution validates composability, and internal V2 manifest resolution returns only ordered Question/QuestionVersion IDs and type codes.

## Assessment Service

- Assessment and AssessmentVersion authoring, revision, publication, retirement, and closing lifecycle.
- Published AssessmentVersions reference only immutable published ChallengeVersions through local ordered ID manifests; no Challenge or Question content is copied into Assessment data.
- Timing supports optional UTC availability windows and optional attempt durations. The future attempt deadline rule is `min(startedAt + duration, availableUntil)` when both exist.
- Policy envelopes support V1 `UNTIMED`/`FIXED_DURATION`, `MAX_ATTEMPTS`, and `IMMEDIATE`/`MANUAL`/`SCHEDULED` result release policies.
- Anonymous public catalogue APIs expose only current `PUBLIC` + `PUBLISHED` AssessmentVersion summaries through an Assessment-owned PostgreSQL projection. The projection is built during new publication and has an explicit, disabled-by-default backfill runner for eligible historical rows.

### Attempt Slice 1

- Authenticated candidates can start an `IN_PROGRESS` Attempt against a published AssessmentVersion with a required idempotency key.
- AttemptItems freeze the complete ordered ID-only route: assessment/challenge/question version references, positions, and question type codes.
- Start enforces availability, max-attempt policy, deadline calculation, active-attempt uniqueness, idempotency, and atomic expiration checks.
- The development-only `AttemptStartEntitlementPort` adapter grants each new user three complimentary `assessment.attempt.start` reservations. A resume does not consume another start; reservation reconciliation is durable for recovery after local persistence.

### Candidate-content delivery

- An Attempt owner can retrieve content only while their Attempt is `IN_PROGRESS` and before its deadline.
- Assessment Service rebuilds the ordered route from Challenge Service's internal manifest API and requests safe exact-version content from Question Service.
- Assessment Service persists no Question content. Browser responses never include correct MCQ answers/explanations, hidden tests, scoring internals, or execution limits.

### Candidate response autosave

- Candidates can save and resume MCQ selections or coding language/source code against the immutable AttemptItem route.
- Saves require idempotency and client-mutation identifiers plus an expected response version; retries replay safely and stale writes return a conflict.
- PostgreSQL is authoritative. Redis is intentionally not used, and the autosave path does not call Question or Challenge Service.

### Public Assessment Catalogue

- Anonymous list and detail APIs return only current `PUBLIC` + `PUBLISHED` AssessmentVersion summaries from an Assessment-owned PostgreSQL projection.
- The projection is created during publication, excludes manifests and protected Question data, and can be backfilled only by an explicit disabled-by-default maintenance configuration.
- The Next.js BFF calls these public endpoints without forwarding a candidate JWT. Public catalogue pages support safe search, assessment-type and availability filtering, plus keyset cursor pagination.

### Next.js candidate flow

- Login, registration, logout, `/users/me`, candidate navigation, dashboard, attempt start, candidate content, and response autosave use same-origin BFF routes.
- JWT access tokens remain in HttpOnly cookies and are never exposed to client JavaScript. Public and authenticated candidate navigation remain separate; `/admin` remains isolated for ADMIN users.

## Local backup workflow

Use `D:\MockArena\scripts\Backup-MockArenaPostgres.ps1` to create a custom-format logical `pg_dump` in `D:\MockArena\backups`. It checks Docker/container/PostgreSQL readiness, verifies the dump with `pg_restore --list`, and writes a SHA-256 sidecar. Restore is deliberately explicit through `Restore-MockArenaPostgres.ps1 -ConfirmRestore` and requires interactive confirmation.

`/backups/` is excluded from Git. Docker volumes are not recreated by either script.

## Final verification on 2026-09-12

- Question Service: `22` tests passed, `0` failures, `0` errors.
- Challenge Service: `15` tests passed, `0` failures, `0` errors.
- Identity Service: `3` tests passed, `0` failures, `0` errors.
- Assessment Service: `14` tests passed, `0` failures, `0` errors.
- `git diff --check`: passed.
- Logical backup: `mockarena_20260912_233414.dump`, SHA-256 verified and `pg_restore --list` verified by the backup script.

Latest Assessment Service verification on 2026-09-13: `18` tests passed, `0` failures, `0` errors after V4/V5 response-autosave migrations.

## Local mixed-assessment bootstrap

`scripts/Bootstrap-MockArenaMixedAssessment.ps1` is a guarded local-development bootstrap for a 6-MCQ/4-CODING public assessment. It requires `MOCKARENA_DEVELOPMENT_BOOTSTRAP=true`, authenticates with explicitly supplied local Identity credentials, retains its JWT only in process memory, and stores rerun state under ignored `.local/`.

The bootstrap has not been claimed as successfully executed in this repository state; local services and supplied development credentials are required to run it.

## Intentionally deferred

- Attempt submission, evaluation/code execution, scoring, percentile, leaderboard, and result release execution.
- Monaco editor.
- Identity refresh tokens, logout, MFA, social login, and organization support.
- Redis, Kafka, payments, billing, production entitlement service, and AI skill diagnosis.

## Next development milestone

Implement assessment submission/evaluation and result release, followed by the Monaco coding editor, scoring, and percentile/leaderboard capabilities.
