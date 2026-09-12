# ADR 0012: ChallengeVersion publication and internal resolution

- **Status:** Accepted
- **Date:** 2026-09-12

## Decision

Challenge Service publishes only DRAFT ChallengeVersions. Publication resolves the generic Question Service V2 rule, deterministically removes duplicate logical Questions, requires the requested count, and transactionally replaces the draft preview with an ordered ID-only manifest. The version becomes PUBLISHED and Challenge records its `currentPublishedVersionId`.

Published manifests and content are immutable. A PUBLISHED version may make the single lifecycle transition to RETIRED; retirement preserves its manifest and clears the Challenge current-published pointer only when it points to that version. It does not archive the Challenge.

Challenge Service exposes `POST /internal/v1/challenge-versions/resolve`. It returns only a published, complete version's Challenge ID, ChallengeVersion ID, version number, and status. Missing versions return 404; draft, retired, or incomplete versions return 422. The endpoint never returns Question references, rule metadata, or Question content.

## Consequences

- Local publication state changes are atomic and protected by optimistic locking; Question Service resolution is performed before local mutation and rechecked before commit.
- Assessment Service can later validate ChallengeVersion composability without reading Challenge Service tables.
- Retired and historical manifests remain intact for existing consumers while being excluded from new composition.
