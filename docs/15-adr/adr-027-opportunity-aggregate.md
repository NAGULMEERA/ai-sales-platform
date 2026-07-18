# ADR-027: Opportunity Aggregate

**Label:** ADR-008 (Opportunity Aggregate)  
**Status:** Accepted  
**Date:** 2026-07-18

## Context

Commercial intent (amount, quote, win/loss) must not be overloaded onto Lead status alone, and must not become industry-specific deal types in Platform Core.

## Decision

1. **deal-service** owns Opportunity + Quote (Proposal MVP).
2. Opportunity references `customerId` and optional `leadId`; owner is `assignedTo` (manual; no second assignment pool).
3. Quote line items snapshot catalog offer prices; accepted quote can mark opportunity WON.
4. Opportunity status is independent of Lead pipeline stage (eventual consistency via events/workflows later).
5. No industry-specific Opportunity subclasses.

## Consequences

- Real Estate and Automobile share Opportunity/Quote APIs; line items differ only by catalog products.
- Billing remains a separate bounded context (frozen; not redesigned here).
- Advanced negotiation/approval remains out of MVP scope.
