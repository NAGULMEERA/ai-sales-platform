# ADR-029: Conversation Model

**Label:** ADR-010 (Conversation Model)  
**Status:** Accepted  
**Date:** 2026-07-18

## Context

Customer dialogue must be channel-ready (web, WhatsApp later) without storing message bodies on Lead or coupling conversation to an industry.

## Decision

1. **conversation-service** owns conversation threads and messages.
2. Conversation requires `leadId` and/or `customerId` references (no cross-service FKs).
3. Lead timeline **projects** conversation events as activities; message bodies stay in conversation-service.
4. Channel is a field/config — WhatsApp delivery remains notification/capability runtime, not conversation aggregate ownership of Meta APIs.
5. Industry plugins do not own conversation schemas.

## Consequences

- Same conversation APIs for Real Estate and Automobile.
- Timeline/workflow hooks remain generic event consumers.
- Voice/WhatsApp AI loops are future capability work, not core conversation redesign.
