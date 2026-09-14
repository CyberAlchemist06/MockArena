# Architecture

## Overview

MockArena V1 consists of five deployable Java 21 and Spring Boot services, exposed through versioned APIs to a Next.js/TypeScript web application. Services collaborate through explicit APIs and Kafka events. Ranking remains a module in Assessment Service for V1.

## Deployable services

- **Web application** — Next.js UI for candidates and organization administrators.
- **Identity Service** — organization membership, candidate and administrator identities, authentication, authorization, and access rules.
- **Challenge Service** — user-created challenge authoring, sharing/visibility policy, dynamic question selection while drafting, and immutable published challenge versions. Published ChallengeVersions retain only ordered QuestionVersion references.
- **Question Service** — reusable DSA question authoring, visible and hidden test content, and immutable published QuestionVersions. It does not own Challenge or ChallengeVersion data.
- **Assessment Service** — assessment composition and publication, invitations, attempts, result release, scoring, percentiles, and the ranking/leaderboard module. Published assessment versions reference immutable ChallengeVersions.
- **Evaluation Service** — accepts evaluation work asynchronously and operates isolated candidate-code execution environments. It returns evaluation outcomes; it does not own assessment ranking or results presentation.

## Data ownership

PostgreSQL is the durable system of record. Services may use one PostgreSQL cluster initially, but each service owns separate schemas and tables. A service must not read or write another service's internal tables; cross-service state is obtained through versioned APIs or Kafka events. Redis is used for caching and short-lived coordination, never as the authoritative record. Kafka carries asynchronous evaluation and result events.

## Content and ranking semantics

Challenge, question, and assessment content is versioned. A draft ChallengeVersion may use either explicit QuestionVersion references or rule-based criteria evaluated through Question Service. Publishing resolves those criteria and freezes the resulting ordered QuestionVersion manifest. Published ChallengeVersions do not copy QuestionVersion content; Question Service remains its owner. Once published, a version is immutable; changes require a new version. Attempts, submissions, scores, percentiles, and leaderboard entries retain the assessment version they belong to. Percentile and ranking population consists only of completed attempts for the same published assessment version.

## Data and event flow

The web application calls service APIs. Assessment Service records a submission against an active attempt. For V1 MCQ evaluation it publishes an in-process event after the local submission transaction commits, then calls Question Service's narrow protected historical evaluation endpoint. Assessment Service persists only derived result facts. This is not a durable queue: the internal retry endpoint can reconcile a submitted attempt without a result after a transient failure or process crash. For coding items, submission now atomically creates immutable Assessment-owned source snapshots and opaque durable outbox records, but no relay, Evaluation Service, runner, or code execution exists yet. PostgreSQL remains authoritative.

## Operational target

Services will be containerized with Docker and deployed to Kubernetes. Kubernetes is the target deployment platform; Kubernetes resources, delivery configuration, and observability details are intentionally deferred from this initial repository setup.

## Boundaries

Identity, challenges, questions, assessments/ranking, and evaluation own their respective rules and data. Cross-boundary updates use explicit APIs or Kafka events, not shared internal models or database writes. Identity issues short-lived RS256 access tokens; Question, Challenge, and Assessment Services validate them locally with the configured public key and derive creator provenance from `sub`, without accessing the Identity database. Per-user authorization, organization membership, and access rules remain future work.

## Generic assessment content

Question Service is the generic assessment-content owner. DSA is its initial taxonomy domain rather than a platform constraint. QuestionVersions use data-driven taxonomy assignments, locale, optional difficulty scheme/code, and an extensible type code. V1 implements MCQ and CODING handlers; protected answers and hidden tests remain protected content.

Challenge Service selects draft QuestionVersions through Question Service V2 using the same generic taxonomy, type-code, locale, difficulty-profile, and programming-language metadata. It stores only ordered Question and QuestionVersion identifiers.

Challenge Service publishes a draft by resolving that rule into an immutable ordered manifest and records the current published version on its Challenge. Retiring that version clears the pointer without archiving the Challenge. Its protected internal resolver exposes only published, complete ChallengeVersion metadata for future composition.

Custom Challenge composition may use multiple draft selection groups. Challenge Service alone applies the V1 MCQ/CODING count policy, traverses the generic Question catalogue through opaque cursors, deterministically ranks candidates using the stored ChallengeVersion seed, and deduplicates logical Question IDs across groups before freezing the ID-only manifest. Assessment Service remains unaware of selection groups.

Assessment Service composes only ordered ChallengeVersion identifiers. It validates their published/composable state through Challenge Service's internal resolver and freezes that manifest with timing, attempt, and result-release policy snapshots when an AssessmentVersion is published. It neither accesses Challenge tables nor copies Challenge or Question content.

Assessment Service also owns anonymous public Assessment catalogue reads. It stores a safe local publication-time projection containing only public Assessment metadata, policy summaries, availability, and aggregate Question type counts. Public reads query PostgreSQL directly and require a `PUBLIC`/`PUBLISHED` Assessment, a current-published pointer, and a `PUBLISHED` AssessmentVersion. The public API never calls Challenge or Question Service, exposes manifests, or forwards browser identity.

For future Attempt routing, Challenge Service additionally exposes a protected V2 manifest contract for published, complete ChallengeVersions. It returns only the persisted position plus Question/QuestionVersion IDs and Question type code. Assessment Service consumes this service-to-service projection; it never reads Challenge or Question tables and the contract never contains Question content or protected data.

Attempt start persists an Assessment-owned, ID-only route built from that manifest. It uses a replaceable entitlement reservation boundary and durable reconciliation record; no payment or entitlement-provider logic is embedded in Assessment content or Attempt domain logic.

Candidate response autosave is also Assessment-owned. It validates an authenticated candidate against the frozen AttemptItem route and persists only the candidate's MCQ selection or coding language/source code in PostgreSQL. It does not call Question or Challenge Service, does not persist Question content or protected evaluation data, and is versioned/idempotent for concurrent browser requests. Redis is intentionally not used; PostgreSQL remains authoritative.

MCQ evaluation consumes only the frozen AttemptItem QuestionVersion IDs. Question Service provides a purpose-specific internal projection containing correct option identity and immutable scoring-policy data for exact historical `PUBLISHED` or `RETIRED` versions. Assessment Service persists derived `AttemptResult` and `AttemptItemResult` facts, not protected answers or candidate source. A fully MCQ Attempt is `EVALUATED`; a mixed attempt is `PARTIALLY_EVALUATED` until coding evaluation exists. Candidate result reads are owner-scoped and read-only, and derive release visibility from the immutable AssessmentVersion policy.

Executable coding content is opt-in, versioned Question content. The first profile is Java standard input/output only and includes validated hidden tests, comparison rules, limits, and scoring policy. Its protected historical projection is workload-authenticated and never candidate-facing. Assessment snapshots submitted coding source but never hidden tests; a future Evaluation Service will consume the durable outbox, retrieve protected execution data under workload identity, and return only derived facts.
Coding evaluation is split across Assessment (submitted snapshots/outbox), Question (historical protected specification), and Evaluation (durable job/retry/lease preparation). Evaluation never owns Attempt results and never persists candidate source or hidden tests.
