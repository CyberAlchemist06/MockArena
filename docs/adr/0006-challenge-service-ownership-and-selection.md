# ADR 0006: Challenge Service ownership and QuestionVersion selection

- **Status:** Accepted
- **Date:** 2026-09-12

## Decision

Introduce Challenge Service as the owner of user-created `Challenge` and `ChallengeVersion` aggregates. Challenge Service owns challenge lifecycle, version lifecycle, visibility, future organization/access policy, and question-selection rules. Question Service owns only `Question` and `QuestionVersion`, including all question content and visible and hidden tests.

Challenge Service stores Question and QuestionVersion identifiers as cross-service references only. It must not read Question Service persistence or copy question content into ChallengeVersion records. It obtains eligible QuestionVersion metadata through a protected, versioned Question Service API.

A draft ChallengeVersion supports two selection modes:

- `EXPLICIT`: the author supplies exact QuestionVersion references, which Question Service validates.
- `RULE_BASED`: Challenge Service asks Question Service for QuestionVersion candidates matching defined criteria and can dynamically preview a selection while the version remains a draft.

When a ChallengeVersion is published, Challenge Service resolves the selected QuestionVersions, persists the exact ordered manifest, and makes the version immutable. The persisted manifest, rather than a later dynamic selection, is used by Assessment Service and historical consumers. A changed selection or challenge definition requires a new ChallengeVersion.

Organization scope, visibility (`PRIVATE`, `SHARED`, `PUBLIC`, and `ORGANIZATION_ONLY`), and access grants remain part of the Challenge domain. Authentication and authorization enforcement are deferred until Identity/Security integration; `ownerUserId` is provenance metadata until then.

## Rationale

Challenge composition and sharing have a distinct lifecycle from reusable DSA problem content. Separating them makes Question Service the sole authority for executable question definitions while allowing Challenge Service to evolve authoring, organization, and reuse policy independently. Freezing a resolved QuestionVersion manifest at publication preserves reproducibility without duplicating owned question content.

## Consequences

- ADR 0002 is superseded.
- Architecture and domain documentation identify five V1 services and assign Challenge/ChallengeVersion exclusively to Challenge Service.
- Question Service requires a protected catalog/validation API before Challenge Service can resolve dynamic selections; no cross-schema database access is permitted.
- Published ChallengeVersions remain usable as historical references even when their QuestionVersions are no longer eligible for new composition, subject to Question Service retaining published-version retrieval.
- Future Identity integration must enforce creator, organization membership, visibility, and access-grant rules without changing versioned challenge content.
