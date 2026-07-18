# Dual-Industry Vertical Validation Roadmap

**Status:** Active after Architecture Freeze (ADR-030)  
**Industries under test:** Real Estate + Automobile  
**Method:** Capability-by-capability across both industries (not industry-by-industry)

## Principle

```text
Don't validate one industry.
Validate one capability across two industries.
```

## Frozen Platform Core

Identity · Notification · Lead · Catalog · Pipeline · Conversation · Timeline ·  
Workflow · AI Gateway · Opportunity · Marketplace registry · Plugin SDK · Billing

No new public core APIs unless both industries require the same change.

## Vertical sprints (suggested)

| Sprint | Capability | Real Estate proof | Automobile proof | Pass criteria |
|--------|------------|-------------------|------------------|---------------|
| 1 ✅ | Create Lead | `budget`, `location`, `propertyType` via `CreateLeadRequest.attributes` | `vehicle`, `budget`, `financeRequired` via same API | Same Lead API; attributes → `leads.metadata`; automobile + real-estate `leadAttributeKeys` metadata |
| 2 ✅ | Qualification | `LEAD_QUALIFY_REAL_ESTATE` vars: budget, location, timeline | `LEAD_QUALIFY_AUTOMOBILE` vars: budget, vehicle, financeRequired, exchange | Same `POST /ai/execute` + `POST /leads/{id}/ai-qualification`; AI recommends, Lead decides |
| 3 ✅ | Pipeline | `REAL_ESTATE_SALES_V1`: New→Qualified→Visit→Negotiation→Booked | `AUTOMOBILE_SALES_V1`: New→Qualified→Test Drive→Quotation→Finance→Booked | Same `POST /pipelines/ensure` + `LeadStateMachine`; labels/edges differ; codes stay `LeadStatus` |
| 4 ✅ | Matching | `attributeFilters`: bedrooms, bathrooms, location | `attributeFilters`: make, model, year | Same `POST /matches`; generic attribute filters; plugin `matchAttributeKeys` |
| 5 ✅ | Opportunity + Quote | quote catalog `offerId` (residential / property) | quote catalog `offerId` (vehicle) | Same `POST /opportunities` + `POST /quotes`; no industry quote types; plugins set `quoteLineSource=catalog.offerId` |
| 6 ✅ | Conversation + Timeline + Workflow | subject/followupType `VISIT_FOLLOWUP` | subject/followupType `TEST_DRIVE_FOLLOWUP` | Same conversation + timeline + `CONVERSATION_FOLLOWUP_V1`; no industry conversation types |
| 7 ✅ | Plugin install | `POST .../plugins/real-estate/enable` | `POST .../plugins/automobile/enable` | Same marketplace enable API; metadata-only; `PluginEnabled` for INDUSTRY |

## Vertical validation status

**Complete (Sprints 1–7).** Platform Core stayed frozen: same Lead / AI / Pipeline / Catalog / Deal / Conversation / Workflow / Marketplace contracts for Real Estate and Automobile; industry difference is plugin metadata + attribute/config payloads only.

## Later phases (after vertical proof)

1. Deepen plugin metadata (pipeline/form/prompt/catalog schema config surfaces).
2. Marketplace as products (Premium SKUs) — still config install, not core rewrite.
3. Richer Prompt / Knowledge registries (plugins reference IDs).
4. Additional industries (Healthcare, Education, Insurance) **without** Platform Core changes.
5. Party / Sales Process only if two industries force them.

## Explicit non-goals during validation

- Building Real Estate completely before Automobile
- Party rewrite
- Capability composition platform (V3)
- Dynamic classloading / hot plugin download
- New industry microservices
