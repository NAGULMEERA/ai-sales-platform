# 06-event-standards.md

# Event Engineering Standards

Version: 1.0

## Purpose

Defines event-driven engineering standards for the AI Sales Employee
Platform.

------------------------------------------------------------------------

# Event Principles

-   Events represent completed business facts.
-   Events are immutable.
-   Producers never know consumers.
-   Consumers are idempotent.

------------------------------------------------------------------------

# Event Types

## Domain Events

Internal to a bounded context.

Examples

-   LeadCreated
-   CustomerUpdated

## Integration Events

Shared across contexts.

Examples

-   AppointmentScheduled
-   PaymentCompleted

------------------------------------------------------------------------

# Naming Standards

Use past tense.

Examples

-   LeadQualified
-   DealWon
-   WorkflowCompleted

Avoid command-style names.

------------------------------------------------------------------------

# Event Schema

Every event contains

-   eventId
-   eventType
-   eventVersion
-   aggregateId
-   tenantId
-   correlationId
-   occurredAt
-   payload

------------------------------------------------------------------------

# Versioning

-   Never break consumers.
-   Add optional fields.
-   Use semantic versioning.
-   Deprecate gradually.

------------------------------------------------------------------------

# Kafka Topics

Convention

``` text
<context>.events
```

Examples

-   lead.events
-   customer.events
-   billing.events

------------------------------------------------------------------------

# Outbox Pattern

Persist business data and event in one transaction.

A background publisher sends events to Kafka.

Never publish directly from controllers.

------------------------------------------------------------------------

# Idempotency

Consumers must safely process duplicates.

Use

-   Event ID
-   Idempotency key
-   Processed event store

------------------------------------------------------------------------

# Retry & DLQ

Use

-   Exponential backoff
-   Retry topics
-   Dead Letter Queue

Never retry indefinitely.

------------------------------------------------------------------------

# Replay

Support replay for

-   Recovery
-   Analytics rebuild
-   Bug fixes

Replay processing must remain idempotent.

------------------------------------------------------------------------

# Security

Events must

-   Preserve tenant context
-   Exclude secrets
-   Protect PII
-   Encrypt when required

------------------------------------------------------------------------

# Observability

Track

-   Published events
-   Consumed events
-   Retry count
-   DLQ count
-   Processing latency

Carry correlation IDs end-to-end.

------------------------------------------------------------------------

# Review Checklist

-   Event named correctly
-   Schema versioned
-   Outbox used
-   Consumer idempotent
-   Retry configured
-   DLQ configured
-   Metrics enabled
-   Documentation updated

------------------------------------------------------------------------

# Related Knowledge

-   05-event-driven-architecture.md
-   03-ddd-standards.md
-   09-observability-architecture.md

# End
