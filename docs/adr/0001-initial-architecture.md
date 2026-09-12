# ADR 0001: Initial architecture

- **Status:** Accepted
- **Date:** 2026-09-12

## Decision

Build V1 as five deployable Java 21 and Spring Boot services: Identity Service, Challenge Service, Question Service, Assessment Service, and Evaluation Service. Use a Next.js and TypeScript frontend. Ranking is a module within Assessment Service for V1.

Use PostgreSQL as the durable system of record. Services may share a PostgreSQL cluster initially, but each owns separate schemas and tables and must not write another service's data. Use Redis for cache and ephemeral coordination, and Kafka for asynchronous evaluation and result events. Package workloads with Docker and target Kubernetes for deployment.

## Rationale

The platform needs explicit ownership for identity, challenge composition, question content, assessment/ranking, and untrusted-code evaluation from the outset. It also needs transactional correctness for assessments and results, low-latency reads for active sessions and leaderboards, and asynchronous processing for submissions.

## Consequences

- Each service maintains explicit ownership and avoids direct cross-service persistence access or writes.
- Evaluation is designed as an asynchronous workflow from the outset.
- Ranking is not a separate service in V1; it can be extracted only after its boundary and operational needs justify it.
- Kubernetes is the deployment target, but deployment configuration is not introduced yet.
