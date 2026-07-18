# ADR-021: Platform Core vs Plugin

**Label:** ADR-002 (Platform Core vs Plugin)  
**Status:** Accepted  
**Date:** 2026-07-18

## Context

Without a hard boundary, industry logic leaks into Platform Core (special cases, industry columns, vendor SDKs) or plugins become mini-monoliths that reimplement business rules.

## Decision

| Belongs in Platform Core | Belongs in Plugin |
|--------------------------|-------------------|
| Aggregates & invariants | Attribute schemas / form definitions |
| Public REST contracts | Suggested pipeline stage graphs (config) |
| Persistence & outbox | Prompt IDs / KB category references |
| Deterministic matching foundation | Industry display labels & validation profiles |
| AI Gateway execution | Capability config (e.g. channel endpoints) |
| Tenant install enable/disable (marketplace) | Packaged product metadata |

Rules:

1. Plugins expose **configuration and metadata**, not `qualifyLead()`-style methods.
2. Platform Core services **must not** depend on industry plugin jars.
3. Runtime integrations (SMTP, Meta WhatsApp) stay in owning platform services; capability plugins describe config only.
4. **No new public Platform Core API** without a concrete requirement demonstrated by **two industries**.

## Consequences

- Marketplace registry stores catalog + tenant installation config (`marketplace-service`).
- `plugin-sdk` remains contracts/descriptors only.
- Future Marketplace “Premium” SKUs are products (install → tenant ready), not new core services.
- Capability composition (Qualification/Pipeline/Catalog/Prompt capabilities) is a **future** evolution, not current work.
