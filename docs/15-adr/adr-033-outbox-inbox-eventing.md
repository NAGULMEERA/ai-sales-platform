# ADR-033: Outbox / Inbox Eventing

**Status:** Accepted  
**Date:** 2026-07-19  
**Owner:** platform (common-events)

## Context

Services must publish integration events reliably and consume them idempotently without dual-writes that skip Kafka after a DB commit.

## Decision

1. Each owning service keeps **local** `outbox_events`, `processed_events` (inbox), and `dead_letter` tables (Flyway per service).
2. Business transactions write outbox rows in the same DB transaction; a publisher relays to Kafka.
3. Consumers use `IntegrationEventListener.handleIfType(...)` with inbox claim/idempotency.
4. Event type names are constants on event classes (`XxxEvent.EVENT_TYPE`).
5. Default topic: `aisales.events.default-topic` (typically `aisales-events`).

## Consequences

- No shared event database across services.
- Retries and DLT ownership are per service (see `docs/12-operations/eventing-ops-matrix.md`).
- Consumers must remain idempotent.

## Related code

- `backend/common/common-events/.../IntegrationEventListener.java`
- `backend/common/common-events/docs/INTEGRATION_EVENT_CONSUMER.md`
