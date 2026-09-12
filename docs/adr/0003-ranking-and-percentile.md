# ADR 0003: Ranking and percentile scope

- **Status:** Accepted
- **Date:** 2026-09-12

## Decision

Ranking and leaderboards are a module within Assessment Service for V1. Scores, percentile values, and leaderboard entries are scoped to a published assessment version. The percentile population is completed attempts of that same version only.

Assessment Service persists the authoritative ranking state in its own PostgreSQL schema and may use Redis to accelerate leaderboard reads. Evaluation outcomes update scores asynchronously; Assessment Service then recalculates the affected ranking state.

## Consequences

- Different versions of an assessment never share a leaderboard or percentile population.
- Ranking can later be extracted if independent scale or query needs justify it.
- Before implementation, the product must define score tie-breaking and the exact meaning of a completed attempt.
