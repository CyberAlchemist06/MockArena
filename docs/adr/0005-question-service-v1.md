# ADR 0005: Question Service V1 persistence and protected tests

- **Status:** Accepted
- **Date:** 2026-09-12

## Decision

Question Service owns `question.questions` and `question.question_versions` in the PostgreSQL `question` schema. Question versions use JSONB for tags, examples, language policy, visible tests, hidden tests, scoring rules, and execution limits in V1. Tags and difficulty are versioned catalog metadata. Hidden tests are protected data: ordinary HTTP DTOs and logging-safe `toString` methods omit them, and mapping from entities to HTTP DTOs is explicit.

Question-version numbers are unique per question. JPA optimistic locking protects concurrent version changes; no distributed lock is introduced. A PostgreSQL trigger rejects updates and deletes once a version is published, as defense in depth. `ownerUserId` records provenance only and is not used for authorization before Identity/Security integration.

## Consequences

- Publishing produces content that is application- and database-immutable.
- API consumers must send the latest entity version for mutable operations and receive conflicts for stale writes.
- The internal QuestionVersion catalog is a safe metadata-only read contract for Challenge Service; it returns only published current versions and never exposes executable or test content.
- A future protected service-to-service contract must be designed before Evaluation can access hidden tests.
