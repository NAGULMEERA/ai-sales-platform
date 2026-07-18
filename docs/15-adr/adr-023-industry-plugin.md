# ADR-023: Industry Plugin

**Label:** ADR-004 (Industry Plugin)  
**Status:** Accepted  
**Date:** 2026-07-18

## Context

Industries need distinct attributes, pipelines, prompts, and catalogs. Encoding that as industry microservices or Lead subtypes breaks multi-industry reuse.

## Decision

1. Industry plugins implement `IndustryPlugin` / `PluginDescriptor` (**metadata only**).
2. Typical contributions (config, not code execution):
   - Catalog attribute keys / schemas
   - Suggested pipeline template codes
   - Prompt registry IDs (references)
   - Knowledge category references
   - Form / qualification profile IDs (future)
3. Loading today: descriptors + marketplace catalog seed; enablement per tenant via `marketplace-service`.
4. Loading tomorrow: Marketplace download → register → enable — **same contracts**, no architecture rewrite.
5. Prefer eventual **capability plugins** that industries compose; do not implement capability composition platform-wide until dual-industry pressure appears.

## Consequences

- `real-estate` and future `automobile` plugins must not contain business methods.
- Platform Core stays free of property/vehicle tables.
- Plugin metadata will grow (pipelines, forms, prompts, schemas, validation rules) as config surfaces — still not executable domain logic.
