# 06-deal-lifecycle.md

# Deal Lifecycle

Version: 1.0

## Purpose

Defines the complete lifecycle of a sales deal from opportunity creation
through successful closure or loss.

------------------------------------------------------------------------

# Business Objective

Convert qualified opportunities into successful business outcomes using
AI assistance, workflow automation, and standardized sales processes.

------------------------------------------------------------------------

# Deal Lifecycle

``` text
Opportunity
    │
    ▼
Qualified
    │
    ▼
Proposal
    │
    ▼
Negotiation
    │
    ▼
Approval
    │
    ▼
Booking
    │
    ▼
Payment
    │
 ┌──┴─────────┐
 ▼            ▼
Won         Lost
```

------------------------------------------------------------------------

# Opportunity Creation

Deals may originate from

-   Qualified Leads
-   Existing Customers
-   Referrals
-   Marketing Campaigns
-   Partner Channels

------------------------------------------------------------------------

# Proposal Management

Proposal includes

-   Products
-   Pricing
-   Discounts
-   Validity
-   Terms & Conditions

Proposal versions are maintained.

------------------------------------------------------------------------

# Negotiation

Negotiation may involve

-   Price
-   Timeline
-   Financing
-   Custom Requirements

AI may recommend next best actions.

------------------------------------------------------------------------

# Approval Workflow

Examples

-   Discount Approval
-   Manager Approval
-   Finance Approval

Approval workflows are configurable.

------------------------------------------------------------------------

# Booking & Payment

Booking confirms customer commitment.

Payment states

-   Pending
-   Partial
-   Completed
-   Failed
-   Refunded

------------------------------------------------------------------------

# AI Assistance

AI may assist with

-   Win Probability
-   Risk Assessment
-   Negotiation Suggestions
-   Follow-up Messages
-   Next Best Action

Business decisions remain under application control.

------------------------------------------------------------------------

# Events

Publish

-   DealCreated
-   ProposalGenerated
-   DealNegotiated
-   DealApproved
-   BookingConfirmed
-   PaymentCompleted
-   DealWon
-   DealLost

------------------------------------------------------------------------

# Business KPIs

Track

-   Win Rate
-   Average Deal Size
-   Sales Cycle Duration
-   Proposal Acceptance Rate
-   Payment Success Rate
-   Revenue

------------------------------------------------------------------------

# Engineering Guidelines

Always

-   Preserve deal history
-   Audit approvals
-   Publish events
-   Validate AI recommendations
-   Maintain tenant isolation

Never

-   Skip approval workflow
-   Lose proposal history
-   Update deal status without audit

------------------------------------------------------------------------

# Related Knowledge

-   02-layered-architecture.md
-   05-event-driven-architecture.md
-   06-workflow-engine.md
-   07-ai-architecture.md
-   05-appointment-lifecycle.md
-   07-billing-lifecycle.md

# End
