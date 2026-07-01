# 01-lead-domain.md

# Lead Domain

Version: 1.0

## Purpose

Defines the Lead bounded context using Domain-Driven Design (DDD). It is
the authoritative reference for implementing lead-related business
logic.

------------------------------------------------------------------------

# Aggregate

**Lead** is the aggregate root.

Responsibilities

-   Protect business invariants
-   Manage lifecycle
-   Publish domain events
-   Coordinate child entities

------------------------------------------------------------------------

# Entities

-   Lead (Aggregate Root)
-   LeadContact
-   LeadAssignment
-   LeadQualification
-   LeadSource

------------------------------------------------------------------------

# Value Objects

-   Email
-   PhoneNumber
-   Budget
-   LocationPreference
-   TimeWindow
-   LeadScore

Value objects are immutable.

------------------------------------------------------------------------

# Aggregate Invariants

A Lead:

-   Must belong to exactly one Tenant
-   Must have at least one contact method
-   Cannot be Won and Lost simultaneously
-   Cannot be Assigned before Validation
-   Cannot be Qualified before Capture

------------------------------------------------------------------------

# Domain Services

-   LeadQualificationService
-   LeadAssignmentService
-   LeadScoringService
-   DuplicateLeadDetectionService

------------------------------------------------------------------------

# Repository

``` text
LeadRepository
```

Responsibilities

-   Save Aggregate
-   Find by Id
-   Find by Tenant
-   Find by Status
-   Optimistic locking

------------------------------------------------------------------------

# Domain Events

-   LeadCreated
-   LeadValidated
-   LeadQualified
-   LeadAssigned
-   LeadConverted
-   LeadLost

Events represent facts and are immutable.

------------------------------------------------------------------------

# State Model

``` text
Captured
   │
Validated
   │
Qualified
   │
Assigned
   │
Contacted
   │
Converted
```

------------------------------------------------------------------------

# Business Rules

-   Duplicate leads must be detected.
-   Qualification requires mandatory attributes.
-   Assignment follows configured strategy.
-   Tenant isolation is mandatory.

------------------------------------------------------------------------

# Engineering Guidelines

Always

-   Keep business rules in the aggregate
-   Publish domain events
-   Validate invariants
-   Use value objects

Never

-   Put business logic in controllers
-   Expose entities directly
-   Bypass aggregate methods

------------------------------------------------------------------------

# Related Knowledge

-   02-layered-architecture.md
-   03-bounded-contexts.md
-   02-lead-lifecycle.md
-   05-event-driven-architecture.md

# End
