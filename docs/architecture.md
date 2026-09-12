# Architecture

## Overview

MockArena V1 consists of four deployable Java 21 and Spring Boot services, exposed through versioned APIs to a Next.js/TypeScript web application. Services collaborate through explicit APIs and Kafka events. Ranking remains a module in Assessment Service for V1.

## Deployable services

- **Web application** — Next.js UI for candidates and organization administrators.
- **Identity Service** — organization membership, candidate and administrator identities, authentication, authorization, and access rules.
- **Question Service** — DSA challenge authoring, visible and hidden test content, and immutable published challenge versions.
- **Assessment Service** — assessment composition and publication, invitations, attempts, result release, scoring, percentiles, and the ranking/leaderboard module. Published assessment versions reference immutable challenge versions.
- **Evaluation Service** — accepts evaluation work asynchronously and operates isolated candidate-code execution environments. It returns evaluation outcomes; it does not own assessment ranking or results presentation.

## Data ownership

PostgreSQL is the durable system of record. Services may use one PostgreSQL cluster initially, but each service owns separate schemas and tables. A service must not read or write another service's internal tables; cross-service state is obtained through versioned APIs or Kafka events. Redis is used for caching and short-lived coordination, never as the authoritative record. Kafka carries asynchronous evaluation and result events.

## Content and ranking semantics

Challenge and assessment content is versioned. Once published, a version is immutable; changes require a new version. Attempts, submissions, scores, percentiles, and leaderboard entries retain the assessment version they belong to. Percentile and ranking population consists only of completed attempts for the same published assessment version.

## Data and event flow

The web application calls service APIs. Assessment Service records a submission against an active attempt and requests evaluation asynchronously through Kafka. Evaluation Service executes candidate code outside application-service processes in an isolated sandbox, then emits an evaluation outcome. Assessment Service consumes the outcome, updates the authoritative score and ranking records for the assessment version, and serves results and leaderboards. Redis can accelerate read paths, while PostgreSQL remains authoritative.

## Operational target

Services will be containerized with Docker and deployed to Kubernetes. Kubernetes is the target deployment platform; Kubernetes resources, delivery configuration, and observability details are intentionally deferred from this initial repository setup.

## Boundaries

Identity, questions, assessments/ranking, and evaluation own their respective rules and data. Cross-boundary updates use explicit APIs or Kafka events, not shared internal models or database writes.
