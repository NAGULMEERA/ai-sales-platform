# 07-workflow-patterns.md

# Workflow Implementation Patterns

Version: 1.0

## Purpose

Defines production-ready workflow implementation patterns for
long-running business processes.

------------------------------------------------------------------------

# Workflow Principles

-   Workflows orchestrate, domains decide.
-   Keep business rules inside aggregates.
-   Workflows coordinate services and events.
-   Every workflow is resumable.

------------------------------------------------------------------------

# Workflow Structure

``` text
Trigger
   │
Validate
   │
Execute Step
   │
Publish Event
   │
Wait / Continue
   │
Complete
```

------------------------------------------------------------------------

# State Machine

Typical states

-   Created
-   Running
-   Waiting
-   Paused
-   Completed
-   Failed
-   Compensated

Only valid transitions are allowed.

------------------------------------------------------------------------

# Human Tasks

Use for

-   Discount approval
-   Manager approval
-   Manual verification
-   Compliance review

Human tasks require audit history.

------------------------------------------------------------------------

# Timers

Support

-   Reminder timers
-   SLA timers
-   Escalation timers
-   Retry timers

Timers must survive application restarts.

------------------------------------------------------------------------

# Compensation

Define rollback actions for completed steps.

Example

``` text
Reserve Inventory
↓
Charge Payment
↓
Fail Shipping
↓
Refund Payment
↓
Release Inventory
```

------------------------------------------------------------------------

# Retry Strategy

Use

-   Exponential backoff
-   Maximum retry count
-   DLQ for failed events

Never retry indefinitely.

------------------------------------------------------------------------

# Event Integration

Workflow listens for events and emits events.

Examples

-   LeadQualified
-   AppointmentScheduled
-   PaymentCompleted

------------------------------------------------------------------------

# Persistence

Persist

-   Workflow state
-   Variables
-   Task history
-   Timer state

Support recovery after crashes.

------------------------------------------------------------------------

# Saga Pattern

Use orchestration for cross-module business processes.

Coordinator responsibilities

-   Start saga
-   Track progress
-   Trigger compensation
-   Complete saga

------------------------------------------------------------------------

# Observability

Track

-   Running workflows
-   Failed workflows
-   Step duration
-   Retry count
-   SLA breaches

Include Correlation ID.

------------------------------------------------------------------------

# Review Checklist

-   Valid transitions
-   Compensation defined
-   Timers configured
-   Events published
-   Persistence enabled
-   Audit trail complete

------------------------------------------------------------------------

# Related Knowledge

-   06-workflow-engine.md
-   09-workflow-domain.md
-   06-kafka-patterns.md
-   09-observability-standards.md

# End
