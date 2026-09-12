# ADR 0002: Domain ownership and versioning

- **Status:** Superseded by ADR 0006
- **Date:** 2026-09-12

## Decision

V1 has five deployable services: Identity, Challenge, Question, Assessment, and Evaluation. Identity owns identities, organization membership, and authorization. Challenge owns user-created Challenges, ChallengeVersions, challenge visibility, and question-selection rules. Question owns Questions, QuestionVersions, and test cases only. Assessment owns assessment composition, attempts, scores, result release, and ranking. Evaluation owns the evaluation workflow and execution environments.

Challenge, Question, and Assessment content is versioned. A ChallengeVersion may dynamically select QuestionVersions while it is a draft, using `EXPLICIT` or `RULE_BASED` selection. Publishing resolves and freezes an exact ordered QuestionVersion manifest without copying QuestionVersion content. A published challenge, question, or assessment version is immutable. Publishing a change creates a new version; an AssessmentVersion references the exact published ChallengeVersions it contains. Attempts and derived results retain their AssessmentVersion.

Services may share a PostgreSQL cluster initially, but each owns separate schemas and tables. No service writes another service's data; collaboration uses versioned APIs or Kafka events.

## Consequences

- Historical attempts and results remain reproducible after content changes.
- Cross-service references use stable identifiers and versions, rather than copied mutable records.
- Database schema ownership must be enforced in application access and migrations.
