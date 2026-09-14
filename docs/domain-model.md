# Domain model — V1

## Model rules

The model is split by service ownership. Cross-service relationships use stable IDs and version IDs delivered through APIs or events; they are not database foreign keys across service schemas. PostgreSQL is authoritative for each owner's data.

A **Challenge** is a reusable, user-created learning or assessment asset. It may be private, shared, organization-only, or public for authorized reuse; candidate-created and shared challenges use the same first-class model as administrator-created challenges. A **Question** is a reusable DSA problem unit. Content is referenced through immutable published versions: an AssessmentVersion selects ChallengeVersions, and a published ChallengeVersion stores an ordered manifest of QuestionVersion references without copying QuestionVersion content.

## User

- **Purpose:** Represents an authenticated person acting as a candidate, challenge author, or organization administrator.
- **Owner:** Identity Service.
- **Important fields:** `userId`, display name, verified email or external identity reference, status, organization memberships, and role assignments.
- **Lifecycle/status:** `active`, `suspended`, or `deactivated`. Membership and roles may change independently of the user record.
- **Relationships:** Creates Challenges; starts Attempts; is the candidate represented by Scores and LeaderboardEntries. Other services retain `userId` references only.
- **Mutable data:** Identity profile, status, memberships, and roles are mutable. The stable `userId` is immutable.

## Challenge

- **Purpose:** Reusable container and ownership boundary for one or more DSA Questions, authored by a user and available for later assessment composition.
- **Owner:** Challenge Service.
- **Important fields:** `challengeId`, `createdByUserId`, optional organization scope, visibility (`PRIVATE`, `SHARED`, `PUBLIC`, or `ORGANIZATION_ONLY`), lifecycle status, current published-version reference, and timestamps. Title and summary belong to the versioned content snapshot.
- **Lifecycle/status:** `draft`, `published`, or `archived`. Publishing makes a selected ChallengeVersion available for reuse; archiving stops new use without rewriting historical references.
- **Relationships:** Has many ChallengeVersions; belongs to its creator and optional organization scope; may have explicit user or organization access grants when shared. AssessmentVersions reference ChallengeVersions, never the mutable Challenge directly.
- **Mutable data:** Ownership scope, visibility, access grants, lifecycle status, and current-version reference are mutable. `createdByUserId` is derived from the authenticated Identity JWT subject when a Challenge is created. Organization and access concepts are modeled now but are not enforced until per-user/organization authorization is added. Published content is not stored as mutable Challenge data.

## ChallengeVersion

- **Purpose:** A versioned snapshot of a Challenge used for publishing and reuse.
- **Owner:** Challenge Service.
- **Important fields:** `challengeVersionId`, `challengeId`, version number, title/description snapshot, authoring metadata, selection mode (`EXPLICIT` or `RULE_BASED`), ordered resolved `questionVersionIds`, publication metadata, and status.
- **Lifecycle/status:** `draft`, `published`, or `retired`. A draft may be edited and may dynamically preview QuestionVersions through Question Service. Publishing resolves its selection to an exact ordered manifest and makes it immutable. Retiring prevents new composition while preserving existing use.
- **Relationships:** Belongs to one Challenge and references one or more QuestionVersions by stable ID. An AssessmentVersion composes exact ChallengeVersions.
- **Immutable data:** All content, ordering, selection rules, and resolved QuestionVersion references are immutable after publication. QuestionVersion content remains owned by Question Service and is never copied into a ChallengeVersion. Corrections create a new version.

Challenge Service may expose the published manifest internally as ordered routing metadata (`position`, `questionId`, `questionVersionId`, `questionTypeCode`) to Assessment Service. This projection contains no Question content and is not a browser API.

## Question

- **Purpose:** Reusable DSA problem identity, separating a durable problem record from its versioned executable content.
- **Owner:** Question Service.
- **Important fields:** `questionId`, creator reference, current version reference, internal tags, and lifecycle status.
- **Lifecycle/status:** `draft`, `published`, or `archived`. A published QuestionVersion can be selected by a ChallengeVersion.
- **Relationships:** Has many QuestionVersions. ChallengeVersions reference QuestionVersions by ID through Question Service APIs; Question Service does not persist Challenge relationships.
- **Mutable data:** Current-version reference, internal tags, and lifecycle status may change. Published problem content is immutable in QuestionVersion.

## QuestionVersion

- **Purpose:** Immutable, gradable definition of a DSA problem.
- **Owner:** Question Service.
- **Important fields:** `questionVersionId`, `questionId`, version number, title, normalized tags, difficulty, prompt, constraints, examples, supported-language policy, visible and hidden test definitions or protected test references, scoring rules, and execution limits. New executable coding versions additionally carry a versioned, validated platform execution specification; legacy coding versions remain non-executable.
- **Lifecycle/status:** `draft`, `published`, or `retired`. Only published versions may be included in a published ChallengeVersion.
- **Relationships:** Belongs to one Question; is selected by a ChallengeVersion; is indirectly included in AssessmentVersions, Submissions, and Evaluations through the selected ChallengeVersion.
- **Immutable data:** After publication, title, tags, difficulty, prompt, tests, scoring, supported languages, and execution limits cannot change. A revision creates a new QuestionVersion. Question Service exposes only a safe catalog projection of published current QuestionVersions for Challenge Service selection; it omits all executable and test content.

## Assessment

- **Purpose:** Reusable container for an assessment and its publication lifecycle. An Assessment may be created by an individual user, an organization, or the MockArena system.
- **Owner:** Assessment Service.
- **Important fields:** `assessmentId`, `creatorType` (`USER`, `ORGANIZATION`, or `SYSTEM`), optional `creatorUserId`, optional `organizationId`, title/summary, visibility (`PRIVATE`, `SHARED`, `PUBLIC`, or `ORGANIZATION_ONLY`), access policy, current version reference, and lifecycle status. `creatorUserId` is required for `USER`; `organizationId` is required for `ORGANIZATION`; system-created assessments need neither. The other identifier remains optional where it supplies provenance or scope.
- **Lifecycle/status:** `draft`, `published`, `closed`, or `archived`. Closing stops new Attempts; archiving preserves history.
- **Relationships:** May be associated with a creator User and/or an organization according to `creatorType`; has many AssessmentVersions, invitations/access grants, and Attempts. AssessmentVersions reference Challenge Service's ChallengeVersions.
- **Mutable data:** Access policy, lifecycle status, and current-version reference may change. `createdByUserId` is derived from the authenticated Identity JWT subject. Published assessment content lives only in AssessmentVersion.

## AssessmentVersion

- **Purpose:** Immutable assessment definition used to run and rank attempts.
- **Owner:** Assessment Service.
- **Important fields:** `assessmentVersionId`, `assessmentId`, version number, ordered ChallengeVersion references (`challengeId`, `challengeVersionId`, and version number), assessment type code, availability window (`availableFrom`/`availableUntil` UTC instants), optional attempt duration, timing policy envelope, attempt policy envelope, result-release policy envelope, publication metadata, and status.
- **Lifecycle/status:** `draft`, `published`, `closed`, or `retired`. A candidate may start an Attempt only at or after `availableFrom` and before `availableUntil` when configured. A started Attempt's deadline is the earlier of its duration deadline and `availableUntil`. Closing prevents future starts without rewriting the immutable snapshot.
- **Relationships:** Belongs to one Assessment; composes exact ChallengeVersions; has Attempts, Scores, and LeaderboardEntries. It receives Question/Challenge data through service contracts, not cross-schema reads.
- **Immutable data:** Once published, composition, order, timing, access, scoring, and release rules are immutable. Changes require a new AssessmentVersion.

### Public catalogue projection

Assessment Service derives an Assessment-owned safe public projection when an AssessmentVersion is published. It contains title/description, a bounded instructions summary, generic assessment type, timing/availability and policy summaries, plus aggregate Question counts by generic question-type code. It contains no Challenge, Question, version, manifest, protected-content, creator, candidate, or scoring identifiers/data. Public discovery only returns the Assessment's current `PUBLIC` and `PUBLISHED` version.

## Attempt

- **Purpose:** A candidate's bounded session for one AssessmentVersion.
- **Owner:** Assessment Service.
- **Important fields:** `attemptId`, `assessmentVersionId`, `candidateUserId`, start/end timestamps, deadline, status, and eligibility/result-release state.
- **Lifecycle/status:** `created`, `in_progress`, `submitted`, `completed`, `expired`, or `cancelled`. `completed` denotes an attempt whose final evaluation outcomes have been applied and which can enter the version's percentile population.
- **Relationships:** Belongs to one AssessmentVersion and candidate; has many Submissions; produces Scores and, when eligible, a LeaderboardEntry.
- **Mutable data:** The active attempt state, timestamps, and submission collection change until finalization. Its assessment-version reference and historical outcome are immutable once completed.

### Candidate response autosave

Assessment Service persists an Attempt response against the immutable `(attemptId, globalPosition)` AttemptItem route. An MCQ response contains only the candidate-selected option ID. A coding response contains only the candidate-selected programming language and source code. The response has an optimistic-lock version and is written with an idempotency key/client mutation record. Only the Attempt owner may read or save it while the Attempt is `IN_PROGRESS`; a due Attempt transitions to `EXPIRED` before either operation completes. Responses contain no correctness or grading data and do not copy Question content, hidden tests, or protected answer material.

### Submitted coding snapshots and outbox

On successful submission, Assessment Service records one immutable coding snapshot for every Coding AttemptItem. `ANSWERED` snapshots retain the submitted language, source, response version, source fingerprint, and timestamp; `UNANSWERED` is explicit rather than inferred from a missing row. The same transaction adds an opaque coding-evaluation outbox record. These Assessment-owned records contain no hidden tests or Question execution/scoring specifications and do not execute candidate code.

## Submission

- **Purpose:** A candidate's code submission for a QuestionVersion during an Attempt.
- **Owner:** Assessment Service.
- **Important fields:** `submissionId`, `attemptId`, `challengeVersionId`, `questionVersionId`, selected language/runtime, source-code reference, sequence number, submitted timestamp, and evaluation state.
- **Lifecycle/status:** `accepted`, `queued`, `evaluating`, `evaluated`, `failed`, or `superseded`. Acceptance is durable before asynchronous evaluation is requested.
- **Relationships:** Belongs to one Attempt and identifies the exact versioned challenge/question it answers. It is correlated with one or more Evaluation records by identifier/event, not a cross-service database join.
- **Mutable data:** Evaluation state and the pointer to the latest applied outcome may change. Submitted source, language, version IDs, and sequence number are immutable after acceptance.

## Evaluation

- **Purpose:** Records an asynchronous evaluation request and its sandbox execution outcome.
- **Owner:** Evaluation Service.
- **Important fields:** `evaluationId`, `submissionId`, `attemptId`, `assessmentVersionId`, `questionVersionId`, sandbox/runtime metadata, input/test outcome summary, resource usage, status, failure reason, and timestamps.
- **Lifecycle/status:** `queued`, `running`, `succeeded`, `failed`, `timed_out`, or `cancelled`. Terminal outcomes are emitted to Assessment Service idempotently.
- **Relationships:** Correlates to one Submission and exact version identifiers through events. It consumes protected test material under Question Service-approved contracts and must never expose it to candidates or application services.
- **Mutable data:** Queue/execution status and in-progress diagnostics may change. A terminal outcome and execution evidence are immutable; a retry is a distinct Evaluation linked to the same Submission.

## Score

- **Purpose:** Authoritative scored result derived from applied Evaluation outcomes.
- **Owner:** Assessment Service.
- **Important fields:** `scoreId`, `attemptId`, `assessmentVersionId`, optional `challengeVersionId` and `questionVersionId`, scope (`question`, `challenge`, or `assessment`), earned and maximum points, calculation version, calculation timestamp, and finality state.
- **Lifecycle/status:** `pending`, `provisional`, `final`, or `invalidated`. Scores become final when the Attempt is completed; a controlled regrade creates a new score calculation rather than altering historical versioned content.
- **Relationships:** Belongs to one Attempt and AssessmentVersion; may summarize a QuestionVersion or ChallengeVersion; feeds the assessment-level LeaderboardEntry.
- **Mutable data:** Pending/provisional scores may be replaced as evaluations arrive. A final score is immutable except for an explicit auditable regrade or invalidation process.

## LeaderboardEntry

- **Purpose:** The ranked, visible result of an eligible completed Attempt for one AssessmentVersion.
- **Owner:** Assessment Service, within its ranking module.
- **Important fields:** `leaderboardEntryId`, `assessmentVersionId`, `attemptId`, `candidateUserId`, final score, percentile, rank, tie-break values, visibility state, and calculated timestamp.
- **Lifecycle/status:** `pending`, `ranked`, `released`, `hidden`, or `superseded`. Only completed Attempts of the same published AssessmentVersion are included in its ranking and percentile population.
- **Relationships:** Belongs to one AssessmentVersion and represents one eligible Attempt; derives from the assessment-level Score. Identity data is displayed using an Identity Service reference or approved projection.
- **Mutable data:** Rank, percentile, visibility, and calculated timestamp may be recalculated as completed attempts arrive. The referenced assessment version, attempt, and underlying final-score snapshot remain immutable for each calculation record; recalculation is auditable.

## Generic content metadata

QuestionVersion metadata is domain-neutral: question-type code, BCP-47 content locale, data-driven taxonomy assignments, optional difficulty scheme/code, programming-language policy when applicable, and a scoring-policy envelope. DSA, country, exam-family, subject, and competency labels are taxonomy data, never Java enums.

ChallengeVersion rule-based selection uses that same generic metadata: taxonomy assignments, question-type codes, difficulty profiles, content locales, programming languages, and a requested count. The draft rule is mutable only while authoring; its resolved ID-only manifest is the composition boundary.

Custom composition stores an ordered list of generic selection groups as draft ChallengeVersion metadata. V1 policy allows one MCQ group and one CODING group with product-specific count limits; this does not constrain Question Service or the persisted group model. Challenge resolves every group through the paged catalogue, reserves logical Question IDs globally, and orders each selected group by a seed-derived deterministic rank.

Publishing replaces the draft preview with a complete, deterministic manifest and updates Challenge's current published-version reference. Retirement preserves the manifest but clears that reference if it is current; it does not archive the Challenge.

## Primary relationship flow

`User` creates and shares `Challenge` in Challenge Service → a draft `ChallengeVersion` dynamically selects QuestionVersions from Question Service using `EXPLICIT` or `RULE_BASED` selection → publication freezes an ordered immutable QuestionVersion manifest → `AssessmentVersion` selects immutable `ChallengeVersions` → candidate `User` creates an `Attempt` → `Submission` may produce an Assessment-owned MCQ result from exact historical QuestionVersion data. Coding evaluation, ranking, and percentile remain future work.

## V1 MCQ result slice

`AttemptItem.questionVersionId` is the historical correctness key. Question Service exposes protected MCQ evaluation data only through an internal purpose-specific projection for exact published or retired versions. Assessment Service stores `AttemptResult` and `AttemptItemResult` derived facts. MCQ-only attempts can become `EVALUATED`; mixed MCQ/CODING attempts remain `PARTIALLY_EVALUATED` with coding items `PENDING` and no final aggregate score. Candidate result reads are owner-scoped, do not trigger evaluation, and are released according to the immutable AssessmentVersion policy.
`evaluation.coding_evaluation_jobs` is an Evaluation-owned durable projection keyed by frozen Attempt item identity and source fingerprint. It has no cross-service database foreign key.
