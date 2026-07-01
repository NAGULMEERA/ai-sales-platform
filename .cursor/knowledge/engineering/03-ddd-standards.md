# 03-ddd-standards.md

# Domain-Driven Design (DDD) Standards

Version: 1.0

## Purpose

Defines the mandatory Domain-Driven Design standards for every bounded
context in the AI Sales Employee Platform.

------------------------------------------------------------------------

# Strategic Design

Every feature belongs to exactly one bounded context.

Never share domain models across contexts.

Communicate through:

-   Domain Events
-   Integration Events
-   Public APIs

------------------------------------------------------------------------

# Tactical Building Blocks

Use

-   Aggregates
-   Entities
-   Value Objects
-   Domain Services
-   Repositories
-   Domain Events
-   Specifications
-   Factories

------------------------------------------------------------------------

# Aggregate Rules

Each aggregate:

-   Has one Aggregate Root
-   Protects invariants
-   Controls state changes
-   Publishes domain events

Never modify child entities directly from outside the aggregate.

------------------------------------------------------------------------

# Entity Standards

Entities:

-   Have identity
-   Encapsulate behavior
-   Avoid public setters
-   Change only through business methods

------------------------------------------------------------------------

# Value Object Standards

Value Objects:

-   Are immutable
-   Compare by value
-   Self-validate
-   Contain no identity

Examples:

-   Money
-   Email
-   PhoneNumber
-   Address

------------------------------------------------------------------------

# Domain Services

Use only when behavior does not naturally belong to an aggregate.

Examples:

-   PricingService
-   AssignmentService
-   ScoringService

------------------------------------------------------------------------

# Repository Standards

Repositories:

-   Persist aggregates only
-   Hide persistence
-   Never contain business rules
-   Return aggregates, not DTOs

------------------------------------------------------------------------

# Domain Events

Events represent completed business facts.

Rules:

-   Immutable
-   Versioned
-   Tenant-aware
-   Published by aggregates

------------------------------------------------------------------------

# Specifications

Use Specification Pattern for reusable business rules.

Examples:

-   ActiveCustomerSpecification
-   EligibleDiscountSpecification
-   QualifiedLeadSpecification

------------------------------------------------------------------------

# Factories

Factories create complex aggregates while ensuring valid initial state.

Never expose partially initialized aggregates.

------------------------------------------------------------------------

# Anti-Patterns

Avoid

-   Anemic Domain Model
-   God Aggregate
-   Shared Database
-   Transaction Scripts
-   Business Logic in Controllers
-   Business Logic in Repositories

------------------------------------------------------------------------

# DDD Review Checklist

-   Correct bounded context
-   Aggregate invariants enforced
-   Value objects immutable
-   Domain events published
-   Repositories clean
-   Specifications reusable
-   No architecture violations

------------------------------------------------------------------------

# Related Knowledge

-   02-layered-architecture.md
-   03-bounded-contexts.md
-   10-common-domain.md
-   01-java-standards.md

# End
