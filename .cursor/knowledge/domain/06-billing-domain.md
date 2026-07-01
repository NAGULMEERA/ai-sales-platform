# 06-billing-domain.md

# Billing Domain

Version: 1.0

## Purpose

Defines the Billing bounded context using Domain-Driven Design (DDD).
This context manages subscriptions, invoices, payments, renewals,
refunds, and financial lifecycle while maintaining consistency,
auditability, and tenant isolation.

------------------------------------------------------------------------

# Aggregate

**Subscription** is the aggregate root.

Responsibilities

-   Manage subscription lifecycle
-   Enforce billing policies
-   Coordinate invoices and payments
-   Publish billing domain events

------------------------------------------------------------------------

# Entities

-   Subscription (Aggregate Root)
-   Invoice
-   Payment
-   Refund
-   BillingPlan
-   PaymentAttempt

------------------------------------------------------------------------

# Value Objects

-   SubscriptionId
-   InvoiceNumber
-   Money
-   BillingCycle
-   PaymentStatus
-   Currency

Value objects are immutable.

------------------------------------------------------------------------

# Aggregate Invariants

A Subscription:

-   Must belong to exactly one Tenant
-   Must reference one Billing Plan
-   Cannot have more than one active billing cycle
-   Cannot be activated without successful payment (unless trial)
-   Cannot process duplicate payments

------------------------------------------------------------------------

# Domain Services

-   BillingCalculationService
-   InvoiceGenerationService
-   PaymentValidationService
-   RenewalService

------------------------------------------------------------------------

# Repository

``` text
SubscriptionRepository
```

Responsibilities

-   Save Aggregate
-   Find by Id
-   Find by Tenant
-   Find Active Subscriptions
-   Find Expiring Subscriptions

------------------------------------------------------------------------

# Domain Events

-   SubscriptionCreated
-   InvoiceGenerated
-   PaymentInitiated
-   PaymentCompleted
-   PaymentFailed
-   SubscriptionRenewed
-   SubscriptionCancelled
-   RefundProcessed

Events are immutable business facts.

------------------------------------------------------------------------

# State Model

``` text
Trial
   │
Active
   │
Renewing
   │
Grace Period
   │
Suspended
   │
Cancelled
```

------------------------------------------------------------------------

# Business Rules

-   Invoice numbers are immutable.
-   Payments must be idempotent.
-   Refunds require validation.
-   Billing history cannot be deleted.
-   Tenant isolation is mandatory.

------------------------------------------------------------------------

# Engineering Guidelines

Always

-   Publish billing events
-   Preserve financial history
-   Audit every payment action
-   Validate payment state transitions
-   Use immutable value objects

Never

-   Modify issued invoices
-   Delete payment history
-   Bypass payment validation
-   Share billing data across tenants

------------------------------------------------------------------------

# Related Knowledge

-   02-layered-architecture.md
-   06-deal-lifecycle.md
-   07-billing-lifecycle.md
-   05-event-driven-architecture.md
-   04-plugin-architecture.md

# End
