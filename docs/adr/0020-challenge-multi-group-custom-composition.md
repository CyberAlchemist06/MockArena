# ADR 0020: Challenge multi-group custom composition

- **Status:** Accepted
- **Date:** 2026-09-14

## Decision

Challenge Service supports draft `selectionGroups` for V1 custom assessment composition. Each group is a generic Question Service V2 metadata filter, but current product policy accepts exactly one of `MCQ` or `CODING` per group, at most one group per type, MCQ total at most 100, CODING total at most 10, and total 10 through 110. These are Challenge product-policy limits, not Question Service constraints.

Challenge stores normalized groups in additive JSONB draft metadata and retains the aggregate requested count for historical composability checks. Legacy rows with an empty group array continue through their existing single-rule adapter. Published manifests remain immutable and authoritative.

Question V2 adds opaque cursor traversal in a canonical order. Challenge traverses every group, globally reserves logical Question IDs, ranks eligible candidates with its persisted seed plus group index and Question identity, and freezes groups in request order with deterministic ranked order inside each group. Resolution retains only a bounded top-K heap for each group while it traverses the full eligible catalogue, while counting all unique candidates for accurate shortage reporting. Publication re-resolves before one local manifest/publication transaction.

## Consequences

- Question Service remains generic and owns no assessment composition limits or selection randomization.
- A shortage fails atomically with `QUESTION_SELECTION_INSUFFICIENT`; counts are never reduced silently.
- Assessment Service continues to compose only published ChallengeVersion IDs and frozen manifests.
- Per-group ranking uses O(N log K) time and O(K + U) memory, where K is the requested count and U is the set of logical IDs needed to deduplicate the traversal.
