# Natural Farming Vertical (Production Plugin)

**Status:** Production-ready metadata vertical  
**Plugin key:** `natural-farming`  
**Industry code:** `NATURAL_FARMING`  
**Module:** `backend/plugins/industry/natural-farming-plugin`  
**Governance:** ADR-023 (Industry Plugin) — metadata only; no industry microservice

## Product intent

Enable natural-farming tenants to sell produce using the existing AI Sales Platform: leads, catalog matching, quotes/orders, WhatsApp capture, AI qualification/recommendation, workflow, payment, search, and analytics — without forking Platform Core.

## Capability → Platform ownership (reuse map)

| Product capability | Platform owner | How Natural Farming expresses it |
|--------------------|----------------|----------------------------------|
| Farm Management | `catalog-service` | Product attributes: `farmId`, `farmName`, `plotId`, `soilType`, `irrigationType`, `region` |
| Product Catalog | `catalog-service` | Products/offers; category `natural-produce`; attribute keys from plugin config |
| Harvest Management | `catalog-service` | Offer/product attrs: `cropType`, `variety`, `season`, `harvestDate`, `yieldKg`, `unit` |
| Inventory | `catalog-service` | Offer attrs: `stockKg`, `availableFrom`, `shelfLifeDays`, `packaging` (no separate inventory service) |
| Order Management | `deal-service` | Opportunity + Quote (`quoteLineSource=catalog.offerId`) |
| Customer Management | `customer-service` | Same Customer APIs |
| WhatsApp Lead Capture | `integration-service` + `whatsapp-channel` | Lead `attributes` via WhatsApp/webhook; plugin `whatsappLeadCaptureEnabled=true` |
| AI Product Recommendation | `catalog-service` recommend + `ai-service` | `POST /recommendations` + prompt `CATALOG_RECOMMEND_NATURAL_FARMING` |
| AI Lead Qualification | `ai-service` + `lead-service` | Prompt `LEAD_QUALIFY_NATURAL_FARMING`; Lead decides |
| Order Workflow | `workflow-service` | `orderWorkflowKey=LEAD_LIFECYCLE_V1` + conversation follow-up |
| Payment Integration | `billing-service` | `paymentCapabilityRef=billing-service` (Stripe/stub unchanged) |
| Delivery Tracking | `conversation-service` + opportunity timeline | `DELIVERY_FOLLOWUP`, delivery attribute keys on timeline/metadata |
| Reports / Dashboards | `analytics-service` | Existing facts; industry filter via tenant plugin + event payload attrs |
| Events | `common-events` / outbox | Same integration events (`LeadCreated`, `QuoteCreated`, `PluginEnabled`, …) |
| Search | `search-service` | Catalog/lead projections; attributes indexed as metadata |
| Pipeline | `lead-service` pipelines | Template `NATURAL_FARMING_SALES_V1` (LeadStatus codes unchanged) |

## Explicit non-goals

- No `natural-farming-service` microservice
- No industry-specific Lead/Quote/Conversation subtypes
- No duplicated notification, AI gateway, search, or billing engines
- No Platform Core API changes required to enable this vertical

## Enablement

```http
POST /api/v1/plugins/natural-farming/enable
```

Marketplace persists installation + config; publishes `PluginEnabled` (`pluginType=INDUSTRY`).

## Pipeline happy path

`New → Qualified → Farm Visit → Negotiation → Order Confirmed → Delivered`  
(Stage **codes** remain platform `LeadStatus`; display names are plugin labels.)

## Lead attribute keys

`buyerType`, `cropInterest`, `volumeKg`, `deliveryRegion`, `organicRequired`, `budget`, `harvestWindow`

## Catalog match keys

`cropType`, `region`, `organicCertified`, `season`

## Prompts (ai-service platform seeds)

| Code | Purpose |
|------|---------|
| `LEAD_QUALIFY_NATURAL_FARMING` | Lead qualification JSON recommendation |
| `CATALOG_RECOMMEND_NATURAL_FARMING` | Rank produce offers (does not invent SKUs) |

## Migrations

| Service | Migration |
|---------|-----------|
| marketplace-service | `V015__seed_natural_farming_industry.sql` |
| ai-service | `V19__seed_natural_farming_prompts.sql` |

## Validation checklist (same APIs as RE/Auto)

1. Enable plugin for tenant  
2. Create lead with natural-farming attributes  
3. Ensure pipeline `NATURAL_FARMING_SALES_V1`  
4. AI qualify with `LEAD_QUALIFY_NATURAL_FARMING`  
5. Catalog match/recommend on crop/region/organic  
6. Opportunity + quote from catalog `offerId`  
7. Conversation follow-up `DELIVERY_FOLLOWUP`  
8. Invoice/payment via billing  
9. Search/analytics consume existing events  

## Related ADRs

- [ADR-023 Industry Plugin](../15-adr/adr-023-industry-plugin.md)
- [ADR-021 Platform Core vs Plugin](../15-adr/adr-021-platform-core-vs-plugin.md)
- [ADR-025 Catalog](../15-adr/adr-025-catalog.md)
- [ADR-030 Architecture Freeze](../15-adr/adr-030-architecture-freeze-and-vertical-validation.md)
