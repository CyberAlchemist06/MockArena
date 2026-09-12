# ADR 0016: ChallengeVersion manifest routing contract

- **Status:** Accepted
- **Date:** 2026-09-12

## Decision

Challenge Service exposes `POST /internal/v2/challenge-versions/manifests` for Assessment Service. The request contains ordered `challengeVersionIds`. It resolves only published, complete ChallengeVersions and returns each Challenge identity/version and its exact persisted manifest order. Each manifest item contains only `position`, `questionId`, `questionVersionId`, and `questionTypeCode`.

Challenge Service remains the manifest owner. It does not read Question Service persistence. Question type is safe routing metadata recorded with a newly published ChallengeVersion manifest from Question Service's existing safe V2 catalog. A Flyway migration adds the metadata column to existing manifests. Older manifests receive `LEGACY_UNSPECIFIED`; their IDs and ordering remain unchanged, and no Question content is inferred or copied.

The endpoint is an internal service-to-service boundary, not a browser-facing API. It must never return titles, stems, options, correct answers, tests, scoring policies, execution limits, selection rules, or any other Question content.

## Consequences

- Assessment Service can later freeze an Attempt's ID-only question route without direct database access or candidate-content delivery.
- Published historical manifests remain immutable in identity and order.
- A future candidate-content contract must remain separately designed and protected; this routing contract cannot be widened for that purpose.
