# MockArena Backlog

## How to use this backlog

This is not a committed roadmap or release promise. Items are intentionally deferred, and product policy may change before implementation. When work is completed, remove it here and record the implemented state in `docs/PROJECT-STATUS.md` as appropriate. Significant architecture decisions should receive their own ADR when work begins.

## Near-term Product

- [ ] Custom Assessment Builder frontend
  - UI for MCQ count, CODING count, taxonomy/topics, difficulty, content locale, programming languages, and duration.
  - Frontend constructs `selectionGroups` only; backend remains authoritative.
  - Omit zero-count groups.
  - UX validation mirrors backend limits but does not replace server validation.

- [ ] Monaco Editor integration
  - Replace the temporary coding textarea with language-aware editing.
  - Preserve existing autosave semantics and do not store JWTs or sensitive state in the editor/client.

- [ ] Submit Attempt workflow
  - Explicit candidate submission, deadline-expiration submission behavior, and idempotent submission.
  - Prevent response mutation after an Attempt reaches a terminal state.

## Assessment Authoring

- [ ] Rich creator assessment authoring workflow
  - Draft/edit/publish UX, timing, availability windows, attempt limits, result-release policy, and Challenge composition preview.

- [ ] Assessment templates
  - Platform-defined templates for common assessment types, such as software engineering, aptitude, certification, and UPSC-style.
  - Templates remain generic metadata/configuration rather than hardcoded domain-specific service logic.

- [ ] Manual question selection / pinning
  - Evaluate explicit QuestionVersion include/exclude controls in addition to rule-based selection.

## Candidate Experience

- [ ] Active attempt listing API
  - The candidate dashboard currently lacks an authoritative active-attempt listing.

- [ ] Candidate results/history dashboard

- [ ] Better question navigation
  - Answered/unanswered status, flagged-for-review, and section/group navigation where applicable.

- [ ] Accessibility and responsive UX pass

- [ ] Safe login `returnTo` flow review
  - Ensure Take Assessment → login/register → intended assessment is consistently preserved.

## Evaluation and Results

- [ ] Evaluation Service
  - MCQ historical evaluation/result persistence now has an Assessment-owned V1 foundation.
  - Coding submission snapshots and an Assessment-owned durable outbox now exist; delivery relay, Evaluation Service jobs, and coding result callbacks remain future work.

- [ ] Secure sandboxed code execution
  - Treat candidate code as hostile.
  - Use an isolated, ephemeral runner with CPU, memory, process, and time limits.
  - Deny network access and isolate the filesystem.
  - Execute as a non-root user, enforce output limits and rate limiting, and destroy the runner after execution.
  - Hidden tests must never reach candidate clients.
  - V1 executable Question contract exists for Java standard I/O only; no runner executes code yet.

- [ ] Coding evaluation and final mixed-assessment scoring
  - MCQ-only scoring and durable derived result facts exist; coding items intentionally remain pending.

- [ ] Advanced result release and review controls
  - V1 read enforcement supports immutable IMMEDIATE/SCHEDULED/MANUAL policy visibility; creator-controlled release workflow and answer review remain future work.

- [ ] Percentile / ranking model

- [ ] Leaderboards, only if product requirements justify them

## AI and Personalization

- [ ] AI weak-area diagnosis
  - Analyze taxonomy/skill performance and explain weaknesses from assessment evidence.
  - Never expose hidden tests or protected question material.

- [ ] Personalized practice recommendations

- [ ] AI-generated study plan

- [ ] Aggregate skill-gap analytics for creators/companies

- [ ] Evaluate AI-assisted question authoring/review
  - Human review and publication controls are required.

## Monetization and Entitlements

Future product policy only; none of the following is implemented behavior.

- [ ] Real Entitlements/Commerce capability
  - Replace the development entitlement adapter with provider-neutral architecture.
  - Keep Question and Challenge domains independent from pricing plans.

- [ ] Free-plan assessment creation policy
  - Current product idea, subject to change: three successfully published/generated assessments monthly; exactly ten questions for a free generated assessment; platform-defined automatic composition; advanced/custom composition reserved for paid capability.
  - Do not consume quota for drafts; consume only after successful publication; failed publication must not consume quota.
  - Do not encode plan limits as database CHECK constraints.

- [ ] Paid custom-assessment entitlement
  - Allow `selectionGroups` within product limits; future plan-specific quotas must be policy/configuration based.

- [ ] Pricing page backed by real plans rather than placeholder content

- [ ] Payment provider integration
  - Keep provider details outside core assessment domains.

## Admin and Analytics

- [ ] ADMIN Console using the existing Identity ADMIN role
  - Do not create a parallel authentication system.

- [ ] User/account administration
- [ ] Question/content administration
- [ ] Challenge/Assessment administration

- [ ] Assessment funnel analytics
  - Catalogue view, detail view, start, completion, and result.

- [ ] Content analytics
  - Question performance, difficulty calibration, abandonment, and response patterns.

- [ ] Company/organization analytics
- [ ] Audit log for privileged/admin actions

- [ ] Privacy-aware geographic analytics
  - Distinguish user-provided country/residence/origin from approximate IP-derived geography.
  - Never silently treat inferred location as nationality.

- [ ] Decide which aggregate growth metrics may be public
  - Never expose sensitive or raw internal analytics.

## Public Discovery

- [ ] Public Question Bank catalogue/API
- [ ] Public company/organization discovery
- [ ] Company information/tie-up pages

- [ ] Search/filter improvements for the public Assessment catalogue
  - Add filters only when backed by authoritative owned metadata.

- [ ] Taxonomy/filter discovery API for assessment authoring
  - Builder filters are currently generic free-form inputs because no authoritative browse API exists.

- [ ] SEO, metadata, and public-sharing improvements

## Security and Anti-Cheating

- [ ] Anti-cheating strategy
  - Tab/window visibility events, suspicious-behavior telemetry, configurable assessment policy, and privacy review.

- [ ] Internal service-to-service authentication
  - Workload identity, service JWT, or mTLS as appropriate.
  - Candidate JWTs must not be forwarded internally.

- [ ] JWT revocation/session strategy if product requirements demand it
  - The current short-lived stateless JWT model may remain sufficient until explicit revocation is needed.

- [ ] Security review of all public/BFF endpoints
- [ ] Rate limiting / abuse protection
- [ ] CSP/security headers/frontend hardening review
- [ ] Secret rotation and production secret management

## Platform and Infrastructure

- [ ] Production deployment architecture
  - Containers/orchestration, environment configuration, secret management, health/readiness, and rolling deployment.

- [ ] Redis evaluation
  - Do not introduce Redis without a concrete requirement.
  - Possible uses: caching, ephemeral coordination, revocation, and rate limiting.
  - PostgreSQL remains authoritative for attempts/responses.

- [ ] Public catalogue caching if real load justifies it

- [ ] Observability
  - Structured logs, metrics, distributed tracing, and alerting.

- [ ] Database operational strategy
  - Automated backup, restore drills, migration process, and retention.

## Developer Experience

- [ ] One-command local startup
  - PowerShell helper and/or Docker Compose to start PostgreSQL, services, and frontend consistently without repeated manual environment configuration.

- [ ] One-command local shutdown/status
- [ ] Repeatable development seed/bootstrap catalogue

- [ ] CI pipeline
  - Question, Challenge, Identity, Assessment, and frontend verification, plus appropriate diff/check equivalents.

- [ ] Dependency/security audit workflow
  - Investigate npm audit findings rather than using `npm audit fix --force` blindly.

## Technical Debt

- [ ] Hibernate composite ID `equals`/`hashCode` cleanup
  - `AttemptItemId`, `AssessmentVersionChallengeId`, and other affected composite IDs.

- [ ] Review historical tracked runtime artifacts
  - Existing historical `*.log` files and Question Service `src.zip` in repository history require separate assessment.
  - Do not delete them casually in this backlog turn.

- [ ] Review internal endpoint exposure before production deployment
- [ ] Revisit catalogue/query indexes with production-scale data
- [ ] Performance/load tests for large Question catalogues and custom composition
- [ ] Recover or reconcile an orphaned published Challenge after creator orchestration failure
  - Current browser-to-BFF workflow spans Challenge and Assessment services without a distributed transaction or cross-service idempotency key.
- [ ] Decide retention/archive strategy for old Assessment, Challenge, and Question versions

## Future Question Types / Extensibility

- [ ] Essay questions
- [ ] Short-answer questions
- [ ] Multi-select MCQ if distinct from current MCQ semantics
- [ ] Case-study/section-based assessments
- [ ] File-upload/project-style assessment questions, if required
- [ ] Rich media question content

- [ ] International/exam-specific taxonomy packs
  - Keep these data-driven.
  - Do not add UPSC/country/exam-specific Java enums to generic services.
