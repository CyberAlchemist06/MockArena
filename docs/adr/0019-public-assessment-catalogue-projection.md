# ADR 0019: Public Assessment Catalogue projection

- **Status:** Accepted
- **Date:** 2026-09-13

## Decision

Assessment Service exposes anonymous `GET /api/v1/public/assessments` and `GET /api/v1/public/assessments/{assessmentId}` APIs. A resource is public only when its Assessment is `PUBLISHED` and `PUBLIC`, its current-published pointer references the version, and that version is `PUBLISHED`. Missing and inaccessible detail resources both return the same not-found response.

Assessment Service stores a safe, Assessment-owned `public_assessment_catalogue` projection at AssessmentVersion publication. It contains public text/policy summaries, availability, and question counts/type counts only. Publication obtains safe question-type routing metadata from Challenge Service's existing internal V2 manifest contract before local state changes; it never copies IDs, manifests, Question content, or protected Question data into the projection. A projection failure rolls back publication.

Public reads query PostgreSQL directly with a narrow projection query plus live lifecycle/current-version predicates. V1 uses bounded title/description search and opaque keyset pagination ordered by `published_at DESC, assessment_id ASC`. Redis and cross-service read calls are intentionally excluded.

Existing eligible rows are backfilled only by an explicit, disabled-by-default Assessment Service maintenance runner. Flyway creates tables and indexes only; it never calls Challenge Service.

## Consequences

- Anonymous browser discovery is safe without making authoring or candidate APIs public.
- Closed, retired, private, shared, organization-only, and superseded versions are hidden immediately by live eligibility predicates.
- Question composition totals are stable publication-time summaries, not a public manifest.
- The Next.js BFF can call the public routes without forwarding an end-user JWT.
