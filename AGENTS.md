# MockArena Engineering Instructions

## Project
MockArena is a global technical assessment platform.
V1 focuses on DSA challenges and mock assessments.

## Core product loop
Create challenge -> Take challenge -> Score -> Percentile -> Compare -> Improve.

## Technology
- Java 21
- Spring Boot
- Maven Wrapper
- PostgreSQL
- Redis
- Kafka
- Docker
- Kubernetes
- Next.js
- TypeScript

## Engineering principles
- Prefer simple designs over unnecessary complexity.
- Keep domain boundaries explicit.
- Services/modules own their data and business rules.
- Do not access another domain's persistence directly.
- Prefer REST for synchronous request/response flows.
- Use Kafka for asynchronous domain events where appropriate.
- Never put secrets in source control.
- New business logic must have automated tests.
- Prefer integration tests with Testcontainers for persistence/infrastructure behavior.
- Keep APIs versioned.
- Record significant architecture decisions as ADRs.

## Codex working rules
- Inspect existing code and documentation before making changes.
- Do not modify unrelated files.
- Explain significant architectural changes before implementing them.
- After implementation, run relevant tests/build checks.
- Do not introduce new dependencies without explaining why.
- Do not generate placeholder code that is presented as production-ready.
- Keep documentation updated when architectural behavior changes.

## Current development approach
Start with a modular backend.
Extract independently deployable microservices as boundaries become stable.