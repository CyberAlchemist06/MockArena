# ADR 0015: AssessmentVersion composition and publication

- **Status:** Accepted
- **Date:** 2026-09-12

## Decision

Assessment Service owns Assessment and AssessmentVersion only. An AssessmentVersion stores an ordered, ID-only manifest of immutable ChallengeVersions. It validates references through Challenge Service's `POST /internal/v1/challenge-versions/resolve` API and never reads Challenge Service tables or copies Challenge or Question content.

Only DRAFT AssessmentVersions can be edited or published. Publication validates the complete ordered manifest and policy envelopes, then atomically persists the manifest, marks the version PUBLISHED, and updates Assessment's current published-version pointer. PostgreSQL triggers make published and retired versions and manifests immutable, except for the legal PUBLISHED-to-RETIRED lifecycle transition.

Timing, attempt, and result-release behavior use generic `policyCode` plus JSON parameters. V1 validates `UNTIMED` and `FIXED_DURATION`; `MAX_ATTEMPTS`; and `IMMEDIATE`, `MANUAL`, and `SCHEDULED`. Availability is separately modeled with optional UTC `availableFrom` and `availableUntil` instants; fixed duration uses optional positive `attemptDurationSeconds`. A future Attempt may start only within the availability window, and its deadline is the earlier of `startedAt + attemptDurationSeconds` and `availableUntil` when both exist. Assessment type is a generic code, initially `STANDARD`; no DSA, country, or exam-family model is embedded in Assessment.

Assessment Service locally validates Identity RS256 JWTs and derives `createdByUserId` exclusively from `sub`.

## Consequences

- One Assessment can compose ChallengeVersions containing MCQ, CODING, or mixed QuestionVersions without knowing Question content.
- ChallengeVersion retirement cannot rewrite already-published AssessmentVersion history.
- Attempt admission, candidate delivery, submission, evaluation, score, percentile, and results remain separate future slices. A version-level `CLOSED` transition prevents future attempts while preserving historical immutable content.
