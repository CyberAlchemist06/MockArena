# MockArena Project Status

## Current architecture

Services:
- Question Service — implemented
- Challenge Service — first slice implemented
- Identity Service — not started
- Assessment Service — not started
- Evaluation Service — not started

## Local ports

- Question Service: 8080
- Challenge Service: 8081

## Infrastructure

- Java 21
- Spring Boot
- PostgreSQL 16 via Rancher Desktop/Docker
- Maven Wrapper
- Testcontainers 2.x
- Kubernetes via Rancher Desktop
- Redis/Kafka not introduced yet

## Question Service

Implemented:
- Question
- QuestionVersion
- draft/published lifecycle
- immutable published versions
- Flyway migrations
- V1 original schema
- V2 catalog metadata
- V3 question types (`MCQ` and `CODING`)
- tags
- difficulty
- supported languages
- internal QuestionVersion catalog API
- development seed data
- PostgreSQL/Testcontainers tests

Catalog endpoint:
POST /internal/v1/question-versions/resolve

Development seed:
14 DSA questions: 12 CODING and 2 MCQ.

## Challenge Service

Implemented:
- Challenge
- ChallengeVersion
- rule-based question selection
- deterministic selection
- Question Service REST client
- PostgreSQL persistence
- composition stores only Question/QuestionVersion IDs
- insufficient selection -> 422

Endpoint:
POST /api/v1/challenges

Local port:
8081

Question Service dependency:
http://localhost:8080

## Verified end-to-end

Successfully created:

"My Tree Challenge"

criteria:
- trees
- MEDIUM
- JAVA
- 1 question

Challenge Service dynamically called Question Service and selected:

Binary Tree Level Order Traversal

The ChallengeVersion was created as DRAFT.

## Important architecture rules

- Services never access another service's database.
- Question Service owns Question/QuestionVersion.
- Challenge Service owns Challenge/ChallengeVersion.
- Published versions are immutable.
- Challenge Service stores QuestionVersion IDs, not copied question content.
- PostgreSQL is source of truth.
- Redis will be added only when required.
- Kafka will be added when asynchronous workflows require it.
- AI generation/skill diagnosis comes later.

## Current next step

Do NOT implement another feature immediately.

First understand Challenge Service code by tracing:

POST /api/v1/challenges

through:

ChallengeController
-> ChallengeApplicationService
-> RestQuestionCatalogClient
-> Question Service
-> DeterministicQuestionSelector
-> repositories
-> PostgreSQL

After understanding that flow:

Implement ChallengeVersion publication so a draft dynamic selection becomes a frozen immutable manifest.

Then proceed toward:
Assessment -> Attempt -> Submission -> Evaluation -> Score -> Percentile -> Leaderboard.
