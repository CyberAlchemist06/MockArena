# Question Service

The Question Service owns versioned DSA question content in the PostgreSQL `question` schema. Its APIs are under `/api/v1/questions`.

Hidden tests are persisted as JSONB for V1, but are deliberately excluded from every HTTP response and entity `toString`. `ownerUserId` is provenance metadata only; no authorization decision is implemented until Identity/Security is integrated.

Published question versions are immutable in both application rules and a PostgreSQL trigger. Mutating version operations require the client’s JPA version value; PostgreSQL's unique `(question_id, version_number)` constraint provides an additional concurrency safeguard. No distributed locking is used.

Health is at `/actuator/health`; Kubernetes-compatible probe paths are `/actuator/health/liveness` and `/actuator/health/readiness`.

Run tests from this directory with `./mvnw.cmd test`. The first run downloads Maven if it is not already cached. Integration tests use Testcontainers and require a running Docker daemon.
