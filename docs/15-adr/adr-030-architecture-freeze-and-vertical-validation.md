# ADR-030: Architecture Freeze and Vertical Dual-Industry Validation

**Status:** Accepted  
**Date:** 2026-07-18

## Context

Platform Core (Identity, Notification, Lead, Catalog, Conversation/Timeline, Workflow, AI Gateway, Opportunity/Deal, Marketplace registry, Plugin SDK) is mature enough that further speculative abstractions (Party, Sales Process, capability composition, richer registries) risk premature generalization.

Building **one industry end-to-end** (e.g. Real Estate only) unconsciously optimizes naming and APIs around that vertical, forcing refactoring when Automobile arrives.

## Decision

### Architecture Freeze

Declare an **Architecture Freeze** on Platform Core public contracts:

| Frozen (evolve only with dual-industry proof) |
|-----------------------------------------------|
| Identity, Notification |
| Lead, Catalog, Pipeline |
| Conversation, Timeline, Workflow |
| AI Gateway (prompts/execute/KB metadata) |
| Opportunity / Quote (deal-service) |
| Marketplace plugin registry + plugin-sdk |
| Billing (existing boundary; no redesign for freeze) |

**Rule:** No new public Platform Core API without a concrete requirement demonstrated by **two industries**.

**Rule:** Industry differences stay in plugin metadata and configuration.

**Rule:** Platform Core evolves only when at least two industries expose the same need.

### Vertical validation (not horizontal industry completion)

Validate **one capability across two industries**, then the next capability:

```text
Lead Creation     → Real Estate + Automobile
Qualification     → Real Estate + Automobile
Pipeline          → Real Estate + Automobile
Matching          → Real Estate + Automobile
Opportunity/Quote → Real Estate + Automobile
Workflow + AI     → Real Estate + Automobile
```

Do **not** complete entire Real Estate then entire Automobile.

### Deferred (roadmap, not freeze work)

- Party model
- Sales Process aggregate (multi-pipeline)
- Capability composition platform (Qualification/Pipeline/Catalog/Prompt capabilities as first-class)
- Marketplace as commercial product catalog (beyond thin registry)
- Dynamic plugin download/loader (design for it; don't build runtime yet)
- Prompt/Knowledge registries as richer multi-dimensional products (IDs already exist; deepen when needed)

## Consequences

- Next engineering focus: dual-industry **Sprint slices**, starting with Create Lead attributes for Real Estate and Automobile on the same Lead API.
- ADRs 020–029 record the frozen decisions for future contributors.
- Any abstraction that survives both industries may enter Platform Core; anything needed by only one stays in the plugin.
