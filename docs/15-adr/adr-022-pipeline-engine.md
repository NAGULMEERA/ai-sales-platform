# ADR-022: Pipeline Engine

**Label:** ADR-003 (Pipeline Engine)  
**Status:** Accepted  
**Date:** 2026-07-18

## Context

Sales journeys differ by industry (visit vs test drive vs admission interview), but Lead state transitions must remain tenant-configurable without hardcoding industry graphs in Platform Core.

## Decision

1. **Lead-service owns** pipeline definitions, stages, allowed transitions, and Lead.`pipelineId`.
2. Pipelines are **tenant-scoped data** (persisted), not industry enums on Lead.
3. `DefaultSalesPipelineDefinition` is a **bootstrap** seed for the default graph — acceptable short-term; long-term definitions come from config/plugin metadata, not Java as the source of truth.
4. Industry plugins may suggest stage names/graphs as **metadata**; they do not execute transitions.
5. Future multi-pipeline industries (residential vs commercial) may motivate a **Sales Process** aggregate above Pipeline — **not implemented now**; wait for dual-industry proof.

## Consequences

- Real Estate and Automobile can define different stage sequences without Lead API changes.
- Vertical validation Sprint (Pipeline): same transition engine, different stage metadata per industry.
- Avoid renaming Pipeline → Sales Process until two industries need multiple pipelines under one process.
