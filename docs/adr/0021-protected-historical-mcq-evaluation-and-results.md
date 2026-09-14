# ADR 0021: Protected historical MCQ evaluation and Assessment-owned results

## Status

Accepted

## Context

AttemptItems freeze exact historical QuestionVersion identifiers, while MCQ correctness must remain protected from candidate and public APIs. Assessment Service needs durable candidate result facts without copying correct answers, source code, or Question evaluation payloads into its database.

## Decision

Question Service exposes `POST /internal/v1/question-versions/evaluation-data` for exact `PUBLISHED` or historical `RETIRED` MCQ versions. The purpose-specific `McqEvaluationData` projection contains only the requested version identity, type, correct option identifier, and immutable scoring-policy snapshot. It is not a general QuestionVersion correctness getter and is never exposed through candidate/public endpoints.

Assessment Service evaluates only frozen `AttemptItem.questionVersionId` values. It stores derived facts in `attempt_results` and `attempt_item_results`, never `correctOptionId`, candidate source code, or protected Question payloads. MCQ-only attempts become `EVALUATED`; mixed MCQ/CODING attempts become `PARTIALLY_EVALUATED` with CODING item results `PENDING` and no final score.

Submission publishes a local event after its local transaction has committed. The synchronous `AFTER_COMMIT` listener attempts best-effort MCQ evaluation and catches failures so submission remains successful. `POST /internal/v1/attempts/{attemptId}/evaluate` is an operational, idempotent recovery path for submitted attempts. It is not browser-facing.

`GET /api/v1/attempts/{attemptId}/result` is read-only, owner-scoped, and makes no Question Service call. A submitted attempt with no result returns `PENDING`. `IMMEDIATE` results are visible once persisted. `SCHEDULED` and `MANUAL` results remain opaque until the immutable AssessmentVersion policy permits release. Release eligibility is derived from that immutable policy; `released_at` is not written merely because a result is read.

## Consequences

The V1 after-commit event is not durable: a process failure after submission commit can leave a submitted attempt without a result. The internal retry endpoint recovers that state. A durable outbox/job runner remains future infrastructure. Coding evaluation, sandbox execution, and richer result review remain out of scope.
