# ADR 0023: Evaluation Service and durable coding-job delivery

Assessment remains authoritative for Attempts, submitted coding snapshots, and results. It relays its transactional coding outbox to Evaluation Service using at-least-once delivery and PostgreSQL claim leases. Evaluation accepts a source-free immutable identity idempotently, retrieves the submitted source from Assessment and exact historical execution data from Question using workload credentials, and retains only job metadata.

Evaluation stores no candidate source or hidden tests. Answered Java V1 jobs stop at `READY_FOR_RUNNER`; unanswered jobs stop at `READY_FOR_RESULT_APPLICATION`. No runner, code execution, result callback, or browser API is included. Retryable dependency failures use capped backoff; permanent integrity/spec failures are infrastructure failures and are never candidate scores.

The current workload-token headers are a transitional application guard. Production deployment must replace or augment them with workload identity and mTLS. A durable runner/result callback remains future work.
