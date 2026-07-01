# 02-customer-domain.md

# Customer Domain

Version: 1.0

## Purpose

Defines the Customer bounded context using Domain-Driven Design (DDD).
It is the authoritative reference for implementing customer-related
business logic and long-term customer management.

------------------------------------------------------------------------

# Aggregate

**Customer** is the aggregate root.

Responsibilities

-   Manage customer lifecycle
-   Protect customer invariants
-   Maintain engagement history
-   Publish domain events

------------------------------------------------------------------------

# Entities

-   Customer (Aggregate Root)
-   CustomerProfile
-   CustomerPreference
-   CustomerAddress
-   CustomerContact
-   CustomerEngagement

------------------------------------------------------------------------

# Value Objects

-   CustomerId
-   Email
-   PhoneNumber
-   Address
-   LanguagePreference
-   CommunicationPreference
-   CustomerScore

Value objects are immutable.

------------------------------------------------------------------------

# Aggregate Invariants

A Customer:

-   Must belong to one Tenant
-   Must have at least one verified contact method
-   Cannot exist without originating from a Lead or approved import
-   Cannot be deleted while active business processes exist

------------------------------------------------------------------------

# Domain Services

-   CustomerOnboardingService
-   CustomerSegmentationService
-   CustomerScoringService
-   CustomerRetentionService

------------------------------------------------------------------------

# Repository

``` text
CustomerRepository
```

Responsibilities

-   Save Aggregate
-   Find by Id
-   Find by Tenant
-   Find by Email
-   Find by Status

------------------------------------------------------------------------

# Domain Events

-   CustomerCreated
-   CustomerUpdated
-   CustomerSegmentChanged
-   CustomerActivated
-   CustomerArchived

Events are immutable and represent completed business facts.

------------------------------------------------------------------------

# State Model

``` text
Created
   │
Onboarded
   │
Active
   │
Engaged
   │
Loyal
   │
Archived
```

------------------------------------------------------------------------

# Business Rules

-   Duplicate customers must be prevented.
-   Customer profile changes are audited.
-   Communication preferences must be respected.
-   Tenant isolation is mandatory.

------------------------------------------------------------------------

# Engineering Guidelines

Always

-   Protect aggregate invariants
-   Publish domain events
-   Preserve customer history
-   Use immutable value objects

Never

-   Modify customer state outside aggregate methods
-   Bypass onboarding validation
-   Share customer data across tenants

------------------------------------------------------------------------

# Related Knowledge

-   02-layered-architecture.md
-   03-bounded-contexts.md
-   03-customer-lifecycle.md
-   05-event-driven-architecture.md

# End
