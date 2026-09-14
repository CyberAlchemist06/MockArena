# ADR 0022: Versioned coding execution contract and submitted-source outbox

## Status

Accepted

## Decision

New executable CODING QuestionVersions may opt into a strict, immutable `coding_execution_spec`. V1 accepts only the platform-controlled `java-21-stdio-v1` profile: Java, `Main.java`, `Main`, and standard input/output. The spec contains a validated version, comparison definition, bounded hidden tests, bounded execution limits, and `ALL_OR_NOTHING` scoring policy. Legacy coding versions without this spec remain candidate-deliverable but are non-executable.

Question Service exposes executable coding material only through the workload-protected internal endpoint `POST /internal/v1/question-versions/coding-evaluation-data`, for exact `PUBLISHED` or `RETIRED` QuestionVersion IDs. It is a purpose-specific projection, not a generic hidden-test accessor. The application requires an externally configured workload token when Question security is enabled; production deployment must additionally provide workload mTLS before any evaluator or runner is enabled. Candidate JWTs are not workload identity.

Assessment submission snapshots every coding AttemptItem in the same transaction as `SUBMITTED` and the submit idempotency record. An answered snapshot contains immutable language, source, response version, SHA-256 fingerprint, and submitted time. An unanswered item receives an explicit `UNANSWERED` snapshot. The same transaction creates one opaque Assessment-owned coding-evaluation outbox record per coding item. The outbox has no source or hidden-test payload and is not delivered in this phase.

## Consequences

No candidate code is executed in this phase. There is no Evaluation Service, runner controller, delivery relay, coding result callback, or sandbox implementation yet. The existing MCQ result state remains unchanged; a future coding callback must merge per-item results and recompute `PARTIALLY_EVALUATED` to `EVALUATED` transactionally.

The Assessment source limit remains 500,000 characters for compatibility. Before public execution is enabled, server-side Assessment, Evaluation, and Runner limits should converge near 128 KiB; frontend limits are UX only.
