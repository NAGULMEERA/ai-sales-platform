# ADR-034: Natural Farming Industry Vertical

**Status:** Accepted  
**Date:** 2026-07-19  
**Deciders:** Product Architecture  

## Context

The platform needs a first production-ready industry vertical beyond Real Estate and Automobile. Natural Farming requires farm/harvest/inventory/order language without violating ADR-023 (industry plugins are metadata only) or ADR-030 (frozen Platform Core).

## Decision

1. Ship `natural-farming` as an **INDUSTRY** plugin (`IndustryPlugin` / marketplace catalog seed).
2. Express farm, harvest, inventory, and delivery concepts as **catalog/lead attribute keys** and pipeline labels — not new aggregates or microservices.
3. Map product capabilities to existing owners:

| Capability | Owner |
|------------|-------|
| Catalog / farm / harvest / inventory attrs | catalog-service |
| Orders / quotes | deal-service |
| Customers | customer-service |
| WhatsApp capture | integration-service + whatsapp-channel |
| AI qualify / recommend | ai-service prompts + catalog recommend |
| Workflow | workflow-service |
| Payment | billing-service |
| Delivery follow-up | conversation-service |
| Reports / search / events | analytics-service / search-service / common-events |

4. Seed prompts `LEAD_QUALIFY_NATURAL_FARMING` and `CATALOG_RECOMMEND_NATURAL_FARMING` in ai-service.
5. Do **not** introduce `natural-farming-service` or industry-specific REST resources under Platform Core.

## Consequences

### Positive

- Third vertical validates plugin metadata extensibility without core API churn.
- Tenants enable via existing marketplace APIs.
- DDD ownership stays clear: plugin owns config; services own aggregates.

### Negative / follow-ups

- Inventory is attribute-level stock (`stockKg`), not a full warehouse domain — sufficient for v1 produce sales; revisit only if two industries require a shared Inventory bounded context.
- Delivery tracking is conversational/timeline metadata, not a logistics TMS.

## Alternatives considered

| Option | Rejected because |
|--------|------------------|
| New natural-farming microservice | Violates ADR-020/021/023; duplicates catalog/deal |
| Industry subtypes on Lead/Quote | Breaks multi-industry reuse |
| Embed farm tables in Platform Core | Pollutes core with vertical schema |

## References

- Plugin module: `backend/plugins/industry/natural-farming-plugin`
- Product doc: [natural-farming-vertical.md](../16-roadmap/natural-farming-vertical.md)
- Marketplace seed: `V015__seed_natural_farming_industry.sql`
- Prompt seed: `V19__seed_natural_farming_prompts.sql`
