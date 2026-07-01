# 04-ddd-implementation.md

# DDD Implementation Patterns

Version: 1.0

## Purpose

Provides implementation patterns for applying Domain-Driven Design in
Spring Boot.

------------------------------------------------------------------------

# Recommended Feature Structure

``` text
lead/
├── api/
├── application/
├── domain/
│   ├── aggregate/
│   ├── entity/
│   ├── valueobject/
│   ├── event/
│   ├── repository/
│   ├── service/
│   ├── factory/
│   ├── specification/
│   └── exception/
└── infrastructure/
```

------------------------------------------------------------------------

# Aggregate Pattern

Aggregate Root responsibilities

-   Protect invariants
-   Control state transitions
-   Publish domain events
-   Hide internal entities

Never expose setters for business state.

------------------------------------------------------------------------

# Entity Pattern

Entities

-   Identity based equality
-   Business behavior
-   No anemic models

------------------------------------------------------------------------

# Value Object Pattern

Characteristics

-   Immutable
-   Self-validating
-   Equality by value

Examples

-   Money
-   Email
-   PhoneNumber

------------------------------------------------------------------------

# Factory Pattern

Use factories when aggregate creation requires:

-   Validation
-   Default values
-   Child entities
-   Initial events

------------------------------------------------------------------------

# Specification Pattern

Encapsulate reusable business rules.

Examples

-   EligibleLeadSpecification
-   ActiveCustomerSpecification

------------------------------------------------------------------------

# Repository Pattern

Repositories persist aggregates only.

Methods

-   save()
-   findById()
-   exists()
-   delete()

No business logic.

------------------------------------------------------------------------

# Domain Service Pattern

Use only when logic spans multiple aggregates.

Examples

-   LeadAssignmentService
-   PricingService

------------------------------------------------------------------------

# Domain Event Pattern

Publish events from aggregate methods.

Examples

-   LeadCreated
-   DealWon
-   PaymentCompleted

------------------------------------------------------------------------

# Application Service Pattern

Coordinates

-   Transactions
-   Aggregate loading
-   Domain execution
-   Event publication

Never implement business rules here.

------------------------------------------------------------------------

# Anti-Patterns

Avoid

-   Anemic domain models
-   Business logic in controllers
-   Business logic in repositories
-   Shared mutable value objects

------------------------------------------------------------------------

# Review Checklist

-   Aggregate protects invariants
-   Value objects immutable
-   Domain events raised
-   Repository clean
-   Services correctly placed

------------------------------------------------------------------------

# Related Knowledge

-   03-ddd-standards.md
-   10-common-domain.md
-   02-spring-boot-standards.md

# End
