# ADR 0002: Domain ownership and versioning

- **Status:** Accepted
- **Date:** 2026-09-12

## Decision

V1 has four deployable services: Identity, Question, Assessment, and Evaluation. Identity owns identities, organization membership, and authorization. Question owns challenge content and test cases. Assessment owns assessment composition, attempts, scores, result release, and ranking. Evaluation owns the evaluation workflow and execution environments.

Question and Assessment content is versioned. A published challenge or assessment version is immutable. Publishing a change creates a new version; an assessment version references the exact published challenge versions it contains. Attempts and derived results retain their assessment version.

Services may share a PostgreSQL cluster initially, but each owns separate schemas and tables. No service writes another service's data; collaboration uses versioned APIs or Kafka events.

## Consequences

- Historical attempts and results remain reproducible after content changes.
- Cross-service references use stable identifiers and versions, rather than copied mutable records.
- Database schema ownership must be enforced in application access and migrations.
