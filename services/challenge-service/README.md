# Challenge Service

Challenge Service owns challenge composition and its PostgreSQL `challenge` schema. This first slice creates a Challenge with one draft, rule-based ChallengeVersion. It calls Question Service's metadata-only catalog API and persists only external `questionId` and `questionVersionId` references.

Run tests with `./mvnw.cmd test`. Integration tests require Docker for PostgreSQL Testcontainers.
