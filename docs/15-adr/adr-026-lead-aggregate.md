# ADR-026: Lead Aggregate

**Label:** ADR-007 (Lead Aggregate)  
**Status:** Accepted  
**Date:** 2026-07-18

## Context

Lead is the entry point of the sales journey. Industry-specific Lead types (`PropertyLead`, `VehicleLead`) would force Platform Core rewrites for every vertical.

## Decision

1. **One generic Lead** in lead-service.
2. Prefer `tenantId` + `pipelineId` (+ attributes/metadata) — **no `industry` column required on Lead**.
3. Industry fields (budget, location, property type vs vehicle, finance required) are **attributes/metadata**, not new aggregates.
4. Assignment pool / ROUND_ROBIN stays in lead-service (not duplicated in deal-service).
5. Convert-to-customer remains a lead-service orchestration via Customer Feign client.

## Consequences

- Vertical validation Sprint 1 (Create Lead): same create API for Real Estate and Automobile; only attribute payloads differ.
- Party model (B2B dealers, brokers, agencies) is **roadmap**, not a freeze blocker.
- Freeze: no new Lead public APIs unless two industries require the same contract change.
