# 05-deal-domain.md

# Deal Domain

Version: 1.0

## Purpose

Defines the Deal bounded context using Domain-Driven Design (DDD). This
context governs the complete sales opportunity lifecycle from proposal
through negotiation, approval, and closure.

------------------------------------------------------------------------

# Aggregate

**Deal** is the aggregate root.

Responsibilities

-   Manage deal lifecycle
-   Protect business invariants
-   Coordinate proposals and approvals
-   Publish domain events

------------------------------------------------------------------------

# Entities

-   Deal (Aggregate Root)
-   Proposal
-   Negotiation
-   Approval
-   PaymentSchedule
-   DealNote

------------------------------------------------------------------------

# Value Objects

-   DealId
-   DealStage
-   Money
-   Discount
-   Probability
-   ClosingDate

Value objects are immutable.

------------------------------------------------------------------------

# Aggregate Invariants

A Deal:

-   Must belong to exactly one Tenant
-   Must reference one Customer
-   Cannot be Won and Lost simultaneously
-   Cannot move to Closed Won without completed approval (if required)
-   Cannot modify accepted proposal versions

------------------------------------------------------------------------

# Domain Services

-   DealScoringService
-   ProposalGenerationService
-   DiscountApprovalService
-   DealForecastService

------------------------------------------------------------------------

# Repository

``` text
DealRepository
```

Responsibilities

-   Save Aggregate
-   Find by Id
-   Find by Tenant
-   Find by Stage
-   Find by Customer
-   Forecast Pipeline

------------------------------------------------------------------------

# Domain Events

-   DealCreated
-   ProposalGenerated
-   DealNegotiated
-   DealApproved
-   DealWon
-   DealLost
-   DealArchived

Events are immutable business facts.

------------------------------------------------------------------------

# State Model

``` text
Opportunity
      │
Qualified
      │
Proposal
      │
Negotiation
      │
Approval
      │
Won
```

Alternative terminal states

-   Lost
-   Archived

------------------------------------------------------------------------

# Business Rules

-   Proposal versions are immutable after submission.
-   Discounts beyond policy require approval.
-   Revenue forecasting uses active deals only.
-   Every stage transition is audited.
-   Tenant isolation is mandatory.

------------------------------------------------------------------------

# Engineering Guidelines

Always

-   Validate stage transitions
-   Publish domain events
-   Preserve proposal history
-   Audit approvals
-   Use immutable value objects

Never

-   Skip approval workflows
-   Modify accepted proposals
-   Bypass aggregate methods
-   Share deals across tenants

------------------------------------------------------------------------

# Related Knowledge

-   02-layered-architecture.md
-   03-bounded-contexts.md
-   06-deal-lifecycle.md
-   05-event-driven-architecture.md
-   06-workflow-engine.md

# End
