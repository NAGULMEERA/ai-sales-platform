# 02-lead-lifecycle.md

# Lead Lifecycle

Version: 1.0

## Purpose

This document defines the complete lifecycle of a lead within the AI
Sales Employee Platform.

It serves as the authoritative business reference for all lead-related
workflows, AI decisions, and engineering implementations.

------------------------------------------------------------------------

# Business Objective

Convert an anonymous prospect into a qualified customer through
automated engagement, AI-assisted qualification, and workflow-driven
follow-up.

------------------------------------------------------------------------

# Lead Sources

Supported sources include:

-   Website
-   Facebook Lead Ads
-   Instagram
-   Google Ads
-   WhatsApp
-   Phone Calls
-   Walk-ins
-   Property Portals
-   CSV Import
-   APIs

Every lead records its acquisition source.

------------------------------------------------------------------------

# Lead Lifecycle

``` text
Captured
    │
    ▼
Validated
    │
    ▼
Qualified
    │
    ▼
Assigned
    │
    ▼
Contacted
    │
    ▼
Interested
    │
    ▼
Appointment Scheduled
    │
    ▼
Negotiation
    │
    ▼
Won / Lost
```

------------------------------------------------------------------------

# AI Qualification

AI evaluates:

-   Budget
-   Timeline
-   Buying Intent
-   Preferred Location
-   Product Interest
-   Conversation Sentiment

The application validates AI recommendations before updating business
state.

------------------------------------------------------------------------

# Assignment Strategy

Lead assignment may use:

-   Round Robin
-   Territory
-   Skill Based
-   AI Recommendation
-   Manual Assignment

Assignment events are published for downstream consumers.

------------------------------------------------------------------------

# Follow-up Strategy

Automated follow-ups include:

-   WhatsApp
-   Email
-   SMS
-   Phone Reminder

Escalate inactive leads based on configurable business rules.

------------------------------------------------------------------------

# Workflow Integration

Lead workflows coordinate:

-   Qualification
-   Assignment
-   Follow-up
-   Appointment Scheduling
-   Escalation

Business rules remain in the domain layer.

------------------------------------------------------------------------

# Event Flow

Example events:

-   LeadCreated
-   LeadValidated
-   LeadQualified
-   LeadAssigned
-   LeadContacted
-   AppointmentScheduled
-   LeadWon
-   LeadLost

Events are immutable and versioned.

------------------------------------------------------------------------

# Business KPIs

Track:

-   Lead Response Time
-   Qualification Rate
-   Assignment Time
-   Appointment Conversion
-   Win Rate
-   Lost Lead Reasons

------------------------------------------------------------------------

# Engineering Guidelines

Always:

-   Preserve tenant context
-   Publish domain events
-   Validate AI output
-   Use workflows for orchestration
-   Audit state changes

Never:

-   Embed business rules in controllers
-   Skip qualification validation
-   Bypass assignment policies

------------------------------------------------------------------------

# Related Knowledge

-   01-platform-business.md
-   02-layered-architecture.md
-   03-bounded-contexts.md
-   05-event-driven-architecture.md
-   06-workflow-engine.md
-   07-ai-architecture.md

# End
