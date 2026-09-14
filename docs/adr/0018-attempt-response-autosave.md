# ADR 0018: Attempt response autosave is Assessment-owned and PostgreSQL-authoritative

- **Status:** Accepted
- **Date:** 2026-09-13

## Decision

Assessment Service owns candidate response persistence. A response is keyed by the immutable Assessment-owned `(attempt_id, global_position)` AttemptItem route, not by mutable Question content or a cross-service foreign key.

V1 stores a type-specific JSON envelope in `assessment.attempt_item_responses`:

- MCQ: `selectedOptionId` only.
- CODING: `programmingLanguage` and candidate `sourceCode` only.

It never stores correct answers, explanations, hidden tests, scoring rules, execution limits, Question content, or grading feedback. PostgreSQL is the authoritative response store. Redis is intentionally not used for active-response state or durability.

Each save requires an `Idempotency-Key`, `clientMutationId`, and expected response version. `assessment.attempt_response_save_requests` durably records the request fingerprint and resulting response route/version. The same key or mutation ID with the same fingerprint replays the saved result; reuse with a different fingerprint is rejected. A stale expected version returns `RESPONSE_VERSION_CONFLICT`; newer data is never silently overwritten.

The save transaction locks the Attempt, derives ownership from the caller's JWT `sub`, expires a due Attempt, requires `IN_PROGRESS`, validates the frozen route and question type, updates the response, and writes the idempotency record atomically. No Question Service or Challenge Service call occurs on this path.

## Consequences

- Autosave is resilient to retries and multiple browser tabs without a distributed transaction.
- Candidate work can be resumed in frozen AttemptItem order.
- Submission, evaluation, grading, score, and feedback remain separate future workflows.
- Source code is returned only to the owner through candidate response APIs and must not be logged or included in error details.
