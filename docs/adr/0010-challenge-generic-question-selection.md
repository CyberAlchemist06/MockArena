# ADR 0010: Challenge generic QuestionVersion selection

- **Status:** Accepted
- **Date:** 2026-09-12

## Decision

Challenge Service uses Question Service's `/internal/v2/question-versions/resolve` metadata-only contract for rule-based draft selection. A rule contains data-driven taxonomy assignments, question-type codes, optional difficulty profiles, content locales, programming languages, and a requested count. It stores no Question content.

Legacy DSA-shaped selection columns remain for compatibility with existing records but are not read or written by current application code. Draft records are backfilled through the additive Challenge Service V2 migration. Published and retired manifests remain unchanged.

## Consequences

- DSA is expressed through taxonomy data such as `content-domain:dsa` and `topic:trees`, not Challenge Service code.
- Selection remains deterministic, removes duplicate logical Questions, and stores only Question and QuestionVersion IDs.
- Challenge Service can support future content domains and Question types without a persistence or Java enum migration.
