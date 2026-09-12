# ADR 0007: QuestionVersion catalog for Challenge Service selection

- **Status:** Accepted
- **Date:** 2026-09-12

## Decision

Question Service exposes `POST /internal/v1/question-versions/resolve` as a metadata-only catalog for Challenge Service. The endpoint filters currently reusable QuestionVersions by `tagsAll`, difficulty, and supported language, and always returns only QuestionVersions whose version and parent Question are published and whose version is the Question's current version.

Catalog metadata is versioned on `QuestionVersion`: normalized lowercase unique tags and difficulty (`EASY`, `MEDIUM`, or `HARD`). The response contains only `questionId`, `questionVersionId`, version number, title, tags, difficulty, and supported languages. It never contains prompts, constraints, examples, tests, scoring rules, or execution limits.

The catalog uses a dedicated projection query rather than loading QuestionVersion entities. Challenge Service selects IDs through this API and stores the resolved IDs in its own published ChallengeVersion manifest; it does not access the `question` schema.

Authentication and workload identity enforcement remain deferred from the implementation scope. The `/internal` route establishes the intended protected service boundary and must be secured before it is exposed outside the trusted service network.

## Consequences

- Rule-based selection can use only currently reusable published QuestionVersions.
- Older published QuestionVersions remain historical data but are excluded once a newer version becomes current.
- The API has no pagination or explicit-ID resolution in V1; those capabilities require a later versioned extension.
- GIN indexes support JSONB containment filters for tags and supported languages.
