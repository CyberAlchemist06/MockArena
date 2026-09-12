# ADR 0004: Asynchronous evaluation and isolated code execution

- **Status:** Accepted
- **Date:** 2026-09-12

## Decision

Evaluation is asynchronous. Assessment Service records submissions and emits evaluation work through Kafka. Evaluation Service processes the work and emits evaluation outcomes for Assessment Service to apply to scores and rankings.

Candidate code must run outside application-service processes in an isolated sandbox. The sandbox must enforce explicit execution time, memory, filesystem, process, and network restrictions, and must not expose application credentials, internal networks, hidden tests beyond the active execution, or production data.

## Consequences

- Submission and result APIs expose pending as well as completed evaluation states.
- Evaluation messages and outcomes must be idempotent and traceable to the submission and assessment version.
- Sandbox implementation and supported-language policy must be specified before candidate execution is enabled.
