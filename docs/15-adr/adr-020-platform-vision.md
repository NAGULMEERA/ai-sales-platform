# ADR-020: Platform Vision

**Label:** ADR-001 (Platform Vision)  
**Status:** Accepted  
**Date:** 2026-07-18

## Context

The AI Sales Platform must support multiple industries (real estate, automobile, education, healthcare, insurance, and others) without forking Platform Core per vertical.

Earlier iterations risked industry-specific Lead types, industry microservices, and AI/vendor coupling. The platform must instead be a **configurable AI sales engine** where industries differ by metadata and configuration.

## Decision

Build a multi-tenant, AI-native SaaS platform with:

1. **Platform Core** — industry-agnostic business capabilities (Lead, Catalog, Pipeline, Opportunity, Conversation, Workflow, AI Gateway, Identity, Notification, Marketplace registry).
2. **Plugins** — metadata and configuration only (pipelines, catalog attribute schemas, prompt IDs, forms). Not executable business methods.
3. **AI as a capability** — recommendations via AI Gateway; Business Services remain system of record.
4. **Events + workflows** — long-running orchestration outside request/response chains.
5. **Validation discipline** — new core abstractions must prove themselves across **at least two industries** before promotion.

Product shape:

```text
Tenant → Install industry/capability plugins → Configure → Sell with AI assistance
```

## Consequences

- No `real-estate-service` / `automobile-service` microservices.
- No industry-specific Lead subclasses in Platform Core.
- Success is measured by running Real Estate and Automobile on the same APIs, not by inventing more layers.
- Roadmap items (Party, Sales Process multi-pipeline, capability composition) stay deferred until dual-industry need appears.
