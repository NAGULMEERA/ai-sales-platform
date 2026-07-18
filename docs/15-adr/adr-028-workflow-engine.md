# ADR-028: Workflow Engine

**Label:** ADR-009 (Workflow Engine)  
**Status:** Accepted  
**Date:** 2026-07-18

## Context

Long-running sales processes (follow-up, approvals, AI orchestration) must not block REST or embed business rules in the orchestrator.

## Decision

1. **workflow-service** coordinates execution state, timers, retries, compensation, human tasks.
2. Business rules and aggregate writes stay in Business Services.
3. Workflows start from REST commands, domain/integration events, or schedules.
4. Industry differences appear as **which workflow definition keys / variables** plugins reference — not as industry code inside the engine.
5. AI and multi-agent sequences are orchestrated by workflow; agents do not call each other directly.

## Consequences

- Conversation follow-up and similar definitions remain platform-owned keys with tenant/plugin config.
- Vertical validation: same engine for Real Estate visit follow-up and Automobile test-drive follow-up.
- No script tasks; no repository access from workflow tasks.
