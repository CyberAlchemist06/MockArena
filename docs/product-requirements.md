# Product requirements — V1

## Purpose

Enable organizations to create and run DSA assessments, and enable candidates to complete challenges under a consistent scoring model.

## In scope

- Organization and candidate accounts with role-based access.
- Authoring reusable DSA challenges and assembling custom assessments.
- Draft, publish, and archive lifecycle for challenges and assessments. Published challenge and assessment content is immutable and receives a version; attempts always reference the published assessment version they used.
- Timed candidate attempts, submissions, and asynchronous automated evaluation against visible and hidden test cases.
- Per-challenge and total assessment scores, including result release to candidates.
- Percentile calculation against completed attempts of the same published assessment version.
- Assessment-version leaderboards with organization-level visibility and documented score tie-breaking rules.
- Candidate and administrator result views.

## Out of scope

- Live interviews, proctoring, billing, marketplace features, and non-DSA assessment types.
- Native mobile applications and advanced plagiarism detection.

## V1 success criteria

An administrator can publish an immutable assessment version, invite candidates, review completed results, and view a trustworthy leaderboard for that version. A candidate can complete an assessment and receive a score and percentile calculated against completed attempts of that same version when results are released.
