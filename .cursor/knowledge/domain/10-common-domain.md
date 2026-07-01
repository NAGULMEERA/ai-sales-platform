# 10-common-domain.md

# Common Domain

Version: 1.0

## Purpose

Defines the shared domain building blocks used across all bounded
contexts in the AI Sales Employee Platform. This document establishes
common patterns, base abstractions, and engineering standards to ensure
consistency across the domain model.

------------------------------------------------------------------------

# Design Principles

-   Domain First
-   Rich Domain Model
-   Domain-Driven Design (DDD)
-   Immutable Value Objects
-   Explicit Business Rules
-   Event-Driven Architecture
-   Multi-Tenant by Design

------------------------------------------------------------------------

# Base Aggregate

Every aggregate root should:

-   Own business invariants
-   Control state transitions
-   Publish domain events
-   Prevent invalid updates

Typical base fields

-   AggregateId
-   Version
-   TenantId
-   Audit Information

------------------------------------------------------------------------

# Base Entity

Every entity contains

-   Identifier
-   Equality by Identity
-   Audit Metadata

Entities are mutable only through aggregate methods.

------------------------------------------------------------------------

# Base Value Object

Characteristics

-   Immutable
-   Equality by Value
-   No Identity
-   Self-validating

Examples

-   Email
-   PhoneNumber
-   Money
-   Address
-   TimeRange
-   Percentage

------------------------------------------------------------------------

# Base Domain Event

Every event contains

-   EventId
-   EventType
-   AggregateId
-   TenantId
-   CorrelationId
-   Timestamp
-   Version

Events are immutable.

------------------------------------------------------------------------

# Shared Domain Services

Common services

-   AuditService
-   TimeProvider
-   IdentifierGenerator
-   TenantContextService
-   CorrelationContextService

Business-specific logic belongs in bounded contexts.

------------------------------------------------------------------------

# Specifications

Use the Specification Pattern for reusable business rules.

Examples

-   ValidEmailSpecification
-   EligibleForDiscountSpecification
-   ActiveSubscriptionSpecification

Specifications should be composable.

------------------------------------------------------------------------

# Policies

Examples

-   AssignmentPolicy
-   PricingPolicy
-   ApprovalPolicy
-   RetryPolicy
-   RetentionPolicy

Policies encapsulate configurable business behavior.

------------------------------------------------------------------------

# Repository Contracts

Repositories should

-   Persist aggregates
-   Hide persistence details
-   Support optimistic locking
-   Respect tenant isolation

Repositories never contain business logic.

------------------------------------------------------------------------

# Domain Exceptions

Create explicit exceptions

-   DomainValidationException
-   BusinessRuleViolationException
-   EntityNotFoundException
-   DuplicateEntityException
-   InvalidStateTransitionException

Avoid generic runtime exceptions.

------------------------------------------------------------------------

# Auditing Model

Capture

-   CreatedBy
-   CreatedAt
-   UpdatedBy
-   UpdatedAt
-   CorrelationId

Critical actions should generate audit events.

------------------------------------------------------------------------

# Tenant Context

Every aggregate must include

-   TenantId
-   OrganizationId (where applicable)

Tenant context must never be lost during processing.

------------------------------------------------------------------------

# Correlation Context

Every request and event carries

-   CorrelationId
-   RequestId
-   TraceId

These identifiers enable end-to-end tracing.

------------------------------------------------------------------------

# Engineering Standards

Always

-   Protect aggregate invariants
-   Use value objects
-   Publish domain events
-   Prefer composition over inheritance
-   Keep business rules inside the domain

Never

-   Put business logic in controllers
-   Share mutable state
-   Bypass aggregate methods
-   Expose persistence concerns to the domain

------------------------------------------------------------------------

# Related Knowledge

-   02-layered-architecture.md
-   03-bounded-contexts.md
-   05-event-driven-architecture.md
-   08-security-architecture.md
-   09-observability-architecture.md

# End
