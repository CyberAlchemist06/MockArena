# ADR 0008: QuestionVersion types and protected MCQ answers

- **Status:** Accepted
- **Date:** 2026-09-12

## Decision

Question Service supports `CODING` and `MCQ` QuestionVersions. `QuestionType` belongs to QuestionVersion so a Question may receive a new version with a different type without rewriting history.

`CODING` versions require coding content: prompt, constraints, examples, supported languages, visible tests, hidden tests, scoring rules, and execution limits. `MCQ` versions require a prompt, at least two uniquely identified options, and one correct option ID that references an option. MCQ and coding-only content cannot be mixed.

The correct MCQ option ID and optional explanation are protected content. They are persisted by Question Service but omitted from every ordinary HTTP response, catalog response, logging-safe `toString`, and exception message. The existing hidden-coding-test boundary remains unchanged.

Published QuestionVersions remain immutable. Changing type requires creation of a new QuestionVersion.

## Consequences

- Existing QuestionVersions are migrated as `CODING`.
- The internal QuestionVersion catalog includes type metadata but no question content or protected answer data.
- Future secured authoring, result-release, and evaluation contracts must introduce explicit protected-data access instead of widening ordinary responses.
