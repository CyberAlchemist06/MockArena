# ADR 0011: Monetization and entitlements architecture

- **Status:** Proposed
- **Date:** 2026-09-12

## Context

MockArena's product services own assessment content, challenge composition, attempts, evaluation, and results. Those services must remain independently deployable and must not contain payment-provider rules or credentials. The platform needs to support individual and organization plans, subscriptions, prepaid credits, promotions, regional pricing, and usage limits without changing the ownership of Questions, Challenges, Assessments, or Evaluations.

## Decision

Introduce a future **Commerce and Entitlements** bounded context. It may begin as a modular backend component and be extracted into a service when its operational boundary is stable. It owns pricing, subscriptions, credit balances, entitlements, usage-metering records, payment-provider adapters, invoices, and commercial audit history.

Product services ask the Entitlements API whether an actor or organization may perform a billable/limited action. They emit auditable usage events after an accepted action. They never calculate prices, store card/payment-provider data, modify balances, or call a payment provider.

### Ownership split

| Concern | Future owner | Responsibility |
|---|---|---|
| Product catalog and regional pricing | Commerce | Sellable products, plan versions, currency/region price books, tax display inputs, credit packs, promotional offers. |
| Subscription | Commerce | Customer account, subscription state, billing period, renewal/cancellation, provider-independent invoice/payment references. |
| Entitlement | Commerce | Effective permissions and limits for a subject, derived from plans, contracts, credits, promotions, and administrative grants. |
| Usage metering | Commerce | Immutable accepted-usage ledger, idempotency, aggregation, quota periods, and overage calculation inputs. |
| Payment-provider integration | Commerce adapter | Provider checkout, webhooks, refunds, disputes, payment method tokens, and reconciliation. |
| Product behavior | Existing domain service | Performs its owned action only after authorization/entitlement decision; emits facts about completed usage. |

### Commercial subject

Every commercial record uses a provider-neutral `billingSubject`:

- `USER` with `userId`, for an individual plan or credits;
- `ORGANIZATION` with `organizationId`, for B2B contracts and pooled quotas.

Identity Service remains the source of user, organization, membership, and authorization facts. Commerce retains stable identifiers only and does not duplicate identity ownership.

### Product catalog and pricing

The catalog is versioned and data-driven. A product may represent a free tier, recurring plan, organization contract, prepaid credit pack, or promotion. A price entry is keyed by product/version, currency, region/market, and effective period. Currency uses ISO 4217 codes; region/market is a data code, not a Java enum. Monetary values are integer minor units plus currency; floating-point amounts are prohibited.

Pricing defines commercial terms, not runtime access decisions. An entitlement grant is the runtime projection of purchased or granted terms, so a price revision never retroactively changes an active subscription or historical invoice.

### Subscriptions, credits, and promotions

Subscriptions reference a catalog version and have provider-neutral lifecycle states such as pending, active, past-due, paused, cancelled, and expired. A subscription grants time-bounded entitlements.

Credits are an append-only balance ledger. Credit grants identify their source: purchase, promotion, manual adjustment, migration, refund, or contract. Credit consumption is linked to the corresponding usage event and is idempotent. Promotional/free credits have an expiry, restrictions, source campaign/reference, and an audit actor; they are never represented by a negative invoice or an undocumented balance mutation.

Free tier access is modeled as a catalog/entitlement grant, rather than a special case in Question, Challenge, Assessment, or Evaluation code.

### Entitlements

An entitlement is a provider-neutral decision input with:

- `capabilityCode`, for example `ai.generate`, `evaluation.execute`, `custom-test.create`, `assessment.publish`;
- optional scope, such as global, organization, workspace, assessment, or content domain;
- limit type: boolean, count, credit-backed, concurrency, or rate/window;
- allowance, consumed/reserved amount, period boundaries, and source grant;
- effective start/end timestamps and an immutable entitlement/grant version.

Capability codes are strings owned by a documented catalog, not Java plan enums. Product services may define their own capability namespace but must not decide plan names or prices.

For actions with a cost or scarce capacity, the Entitlements API supports an atomic `authorize/reserve` operation using a caller-supplied idempotency key. The calling service commits the reservation after successful work or releases it on failure. This prevents concurrent requests from exceeding a quota. Pure read-only feature checks may use a short-lived signed/opaque entitlement decision cache, with Commerce remaining authoritative.

### Auditable usage events

Services emit a stable, provider-neutral `UsageRecorded` fact after accepted work. Commerce deduplicates by `eventId` and `(sourceService, idempotencyKey)`. A usage event contains:

```text
eventId, occurredAt, sourceService, eventType, schemaVersion,
billingSubjectType, billingSubjectId, organizationId?, actorUserId?,
capabilityCode, quantity, unit, idempotencyKey,
resourceType?, resourceId?, correlationId?,
attributes (allowlisted, non-secret metadata)
```

`quantity` is a non-negative decimal/integer in a documented unit. Events are immutable; corrections use compensating adjustment events referencing the original event. Events must never include source code, prompts, hidden tests, MCQ answers, payment details, or unnecessary personal data.

Initial event families can include:

- `ai.generation.requested` / `ai.generation.completed`: model class, output-unit category, request category; never prompt/response text.
- `evaluation.execution.completed`: runtime family, execution count, allocated/actual compute unit category, outcome class; never hidden test data or source code.
- `custom_test.created`: count and test-kind classification; never test payload.
- `assessment.attempt.started` or `assessment.attempt.completed`: count, assessment scope, and organization context where commercially relevant.
- `challenge.published` and `question.published`: count-based quotas where a plan defines them.

The producing service records an outbox entry in the same transaction as its owned business action. A future relay publishes it through Kafka or calls a metering ingestion endpoint. This avoids billing for work that was rolled back and makes delivery retry-safe. Metering must tolerate at-least-once delivery.

### Payment-provider boundary

Commerce exposes provider-neutral checkout, subscription-management, and payment-status APIs. A provider adapter translates those operations to a selected provider. Webhooks terminate in Commerce, are signature-verified, persisted as raw provider audit evidence with redaction controls, deduplicated, and translated into provider-neutral payment/subscription state transitions.

No existing domain service imports a provider SDK, stores a provider customer/payment-method identifier, or receives a provider webhook. Provider replacement therefore changes only the adapter and reconciliation workflows.

## Service interactions

1. A product service authenticates/authorizes through Identity and asks Commerce to authorize or reserve the required capability for its billing subject.
2. Commerce returns allow/deny, applicable grant reference, and reservation ID where needed.
3. The product service performs its owned action transactionally.
4. It commits/releases the reservation and writes an outbox usage fact. Commerce meters the immutable event asynchronously and reconciles any reservation.
5. Commerce calculates entitlement state and exposes a current decision/projection; it does not alter the product service's domain data.

Temporary degradation policy is capability-specific and must be explicit: generally deny new paid, irreversible, or expensive work when entitlement authority is unavailable; permit work only where a cached unexpired allow decision or a documented free/offline allowance exists. All fallback decisions must be auditable.

## Consequences

- Free, subscription, B2B contract, prepaid-credit, promotion, and pay-per-use models share the same entitlement and metering primitives.
- Multiple currencies and regional prices are catalog data, not product-service configuration.
- AI, evaluation, and custom-test limits can be enforced consistently without embedding billing logic in their future services.
- Existing services need only capability checks, reservations for costly work, and sanitized usage facts when monetization is introduced.
- Payment implementation, tax compliance, invoices, refunds, and provider selection are deliberately deferred; this ADR does not authorize billing or payment code.
