# 06-kafka-patterns.md

# Kafka Implementation Patterns

Version: 1.0

## Purpose

Defines production-ready Apache Kafka implementation patterns for the AI
Sales Employee Platform.

------------------------------------------------------------------------

# Messaging Principles

-   Events are immutable
-   Producers are unaware of consumers
-   Consumers are idempotent
-   Business data is persisted before publishing events

------------------------------------------------------------------------

# Topic Design

Naming convention

``` text
<context>.events
```

Examples

-   lead.events
-   customer.events
-   billing.events
-   workflow.events

One business domain per topic.

------------------------------------------------------------------------

# Producer Pattern

Responsibilities

-   Publish integration events
-   Serialize consistently
-   Include metadata
-   Use Outbox Pattern

Never publish directly from controllers.

------------------------------------------------------------------------

# Consumer Pattern

Consumers should

-   Validate events
-   Process idempotently
-   Handle retries
-   Publish downstream events if required

------------------------------------------------------------------------

# Event Schema

Every event contains

-   eventId
-   eventType
-   eventVersion
-   tenantId
-   aggregateId
-   correlationId
-   occurredAt
-   payload

------------------------------------------------------------------------

# Outbox Pattern

Flow

``` text
Business Transaction
        │
        ▼
Database Commit
        │
        ▼
Outbox Table
        │
        ▼
Outbox Publisher
        │
        ▼
Kafka Topic
```

Ensures reliable event delivery.

------------------------------------------------------------------------

# Idempotency

Prevent duplicate processing using

-   Event ID
-   Processed Event Store
-   Business Keys

Consumers must safely handle retries.

------------------------------------------------------------------------

# Retry & DLQ

Implement

-   Exponential Backoff
-   Retry Topics
-   Dead Letter Queue

Never retry indefinitely.

------------------------------------------------------------------------

# Partitioning

Partition by stable business keys

Examples

-   tenantId
-   leadId
-   customerId

Maintain ordering within a partition.

------------------------------------------------------------------------

# Consumer Groups

Each independent business capability has its own consumer group.

Never share offsets across unrelated consumers.

------------------------------------------------------------------------

# Observability

Monitor

-   Consumer Lag
-   Publish Latency
-   Processing Time
-   Retry Count
-   DLQ Size

Include Correlation ID in every event.

------------------------------------------------------------------------

# Review Checklist

-   Topic naming correct
-   Outbox used
-   Consumer idempotent
-   Retry configured
-   DLQ configured
-   Event versioned
-   Metrics enabled

------------------------------------------------------------------------

# Related Knowledge

-   05-event-driven-architecture.md
-   06-event-standards.md
-   09-observability-standards.md

# End
