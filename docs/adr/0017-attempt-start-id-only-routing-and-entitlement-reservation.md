# ADR 0017: Attempt start uses ID-only routing and entitlement reservations

- **Status:** Accepted
- **Date:** 2026-09-12

## Decision

Assessment Service creates an Attempt only for a published and currently available AssessmentVersion. The candidate identity is the Identity JWT `sub`; no request field can supply or override it.

At start, Assessment Service resolves the AssessmentVersion's ordered ChallengeVersion references through Challenge Service's V2 internal manifest API. It persists an `attempt_items` route containing only ordered Challenge, Question, and QuestionVersion identifiers plus question type code. Question content, answers, tests, scoring details, and execution limits are never persisted in Assessment Service.

`Idempotency-Key` is required. A durable start-request row binds its candidate and request fingerprint to the resulting Attempt. A partial unique database index permits only one `IN_PROGRESS` Attempt per candidate and AssessmentVersion. Attempt expiry is checked during start/replay operations and transitions only when `deadlineAt < now`.

Attempt creation uses a reserve/commit/release entitlement port. The local transaction persists the Attempt, route, idempotency record, and a pending reconciliation record. Entitlement commit occurs after that transaction; a retry reconciles a pending commit. A development-profile adapter provides three complimentary starts and is replaceable by a future Entitlements service.
