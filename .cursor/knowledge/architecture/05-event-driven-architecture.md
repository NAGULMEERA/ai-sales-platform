# 05-event-driven-architecture.md

# Event Driven Architecture

Version: 1.0

## Purpose

This document defines the event-driven architecture of the AI Sales
Employee Platform.

The goal is to decouple bounded contexts while enabling scalable,
resilient, and asynchronous communication.

------------------------------------------------------------------------

# Event Philosophy

Events represent business facts.

Examples

-   Lead Created
-   Lead Qualified
-   Customer Created
-   Appointment Scheduled
-   Payment Completed

Events are immutable and never contain business logic.

------------------------------------------------------------------------

# Event Types

## Domain Events

Raised inside a bounded context.

Examples

-   LeadCreated
-   LeadAssigned
-   CustomerUpdated

## Integration Events

Published for other bounded contexts.

Examples

-   AppointmentScheduled
-   InvoicePaid
-   WorkflowCompleted

------------------------------------------------------------------------

# Event Flow

``` text
Aggregate
    │
    ▼
Domain Event
    │
    ▼
Outbox
    │
    ▼
Kafka
    │
 ┌──┼──────────────┐
 ▼  ▼              ▼
Workflow Billing Notification
```

------------------------------------------------------------------------

# Outbox Pattern

Every event is stored in the same transaction as business data.

Benefits

-   No lost events
-   Reliable publishing
-   Retry support

Never publish directly from business logic.

------------------------------------------------------------------------

# Kafka Strategy

Kafka responsibilities

-   Event Distribution
-   Scalability
-   Replay
-   Consumer Isolation

Topic examples

-   lead.events
-   customer.events
-   workflow.events
-   appointment.events
-   billing.events

------------------------------------------------------------------------

# Event Versioning

Rules

-   Never break existing consumers.
-   Add new fields instead of changing existing ones.
-   Use semantic versioning.

------------------------------------------------------------------------

# Idempotency

Consumers must safely process duplicate events.

Use

-   Event IDs
-   Idempotency Keys
-   Processed Event Store

------------------------------------------------------------------------

# Retry Strategy

Retry failures using

-   Exponential Backoff
-   Retry Topics
-   Dead Letter Queue (DLQ)

Never retry forever.

------------------------------------------------------------------------

# Event Replay

Replay supports

-   Disaster Recovery
-   Bug Fixes
-   Analytics Rebuild
-   Workflow Recovery

Replay must remain idempotent.

------------------------------------------------------------------------

# Event Contracts

Each event defines

-   Event Name
-   Version
-   Tenant ID
-   Aggregate ID
-   Timestamp
-   Payload

Never expose internal entities.

------------------------------------------------------------------------

# Observability

Track

-   Published Events
-   Consumed Events
-   Retry Count
-   DLQ Count
-   Processing Latency

Every event carries a Correlation ID.

------------------------------------------------------------------------

# Security

Events must

-   Include Tenant Context
-   Exclude Secrets
-   Protect Personal Data
-   Be Encrypted if Required

------------------------------------------------------------------------

# Engineering Rules

Always

-   Publish immutable events
-   Use Outbox Pattern
-   Validate contracts
-   Keep consumers idempotent
-   Monitor DLQs

Never

-   Publish events directly from controllers
-   Share database tables
-   Depend on consumer implementation

------------------------------------------------------------------------

# Event Checklist

-   Event defined
-   Contract documented
-   Version assigned
-   Outbox implemented
-   Kafka topic created
-   Retry configured
-   DLQ configured
-   Metrics added
-   Tests completed

------------------------------------------------------------------------

# Related Knowledge

-   02-layered-architecture.md
-   03-bounded-contexts.md
-   04-plugin-architecture.md
-   06-workflow-engine.md

# End
