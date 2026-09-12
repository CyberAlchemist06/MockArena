# ADR 0009: Generic assessment content taxonomy

- **Status:** Accepted
- **Date:** 2026-09-12

## Decision

Question Service is a generic assessment-content service. DSA is initial taxonomy data, not a platform concept. QuestionVersions retain immutable identity and versioning while carrying an extensible question-type code, content locale, optional difficulty scheme/code, programming-language policy, scoring-policy envelope, and versioned taxonomy assignments.

V1 supports `MCQ` and `CODING` handlers only. MCQ correct answers and explanations, and coding hidden tests and evaluation details, remain protected data. Taxonomy uses data-driven `(scheme, code)` assignments; country, region, exam family, subject, and competency are never Java enums.

## Consequences

- Existing DSA content is backfilled with `content-domain:dsa`, `topic:<existing-tag>`, `mockarena-v1` difficulty metadata, and `en` locale.
- `/internal/v1` remains available for the existing Challenge Service contract. `/internal/v2` provides generic metadata selection.
- Future question types and taxonomy schemes require new handlers or data, not database enums or a DSA-specific platform model.
