# 06-workflow-engine.md

# Workflow Engine Architecture

Version: 1.0

## Purpose

The Workflow Engine orchestrates long-running business processes across
bounded contexts while keeping business rules inside the Domain Layer.

------------------------------------------------------------------------

# Workflow Philosophy

Workflow coordinates work.

Workflow does NOT own business rules.

Business logic remains inside aggregates and domain services.

------------------------------------------------------------------------

# Core Responsibilities

The Workflow Engine is responsible for:

-   Orchestration
-   State Management
-   Human Tasks
-   Retry
-   Timeout
-   Compensation
-   Escalation
-   Scheduling
-   Event Publishing

------------------------------------------------------------------------

# Workflow Lifecycle

``` text
Business Event
      │
      ▼
Workflow Started
      │
      ▼
State Machine
      │
 ┌────┼─────────────┐
 ▼    ▼             ▼
Task AI Decision Human Task
      │
      ▼
Completed
```

------------------------------------------------------------------------

# State Machine

Every workflow follows explicit states.

Example

``` text
Lead Created
      │
      ▼
Qualified
      │
      ▼
Interested
      │
      ▼
Appointment
      │
      ▼
Won
```

State transitions are validated.

------------------------------------------------------------------------

# Workflow Components

Each workflow contains

-   Definition
-   Version
-   States
-   Transitions
-   Tasks
-   Timers
-   Compensation
-   Escalation
-   Events

------------------------------------------------------------------------

# Workflow Types

Supported

-   Lead Qualification
-   Appointment Booking
-   Customer Follow-up
-   Payment Approval
-   Document Approval
-   AI Review

------------------------------------------------------------------------

# Human-in-the-Loop

Some steps require manual approval.

Examples

-   Sales Manager Approval
-   Discount Approval
-   Loan Verification

Workflow pauses until action is completed.

------------------------------------------------------------------------

# Retry Strategy

Retry transient failures using

-   Exponential Backoff
-   Retry Limits
-   Timeout Handling

Never retry indefinitely.

------------------------------------------------------------------------

# Compensation

If a workflow fails after partial completion

Execute compensation.

Example

Appointment booked

↓

Payment failed

↓

Cancel appointment

↓

Notify customer

------------------------------------------------------------------------

# Timeout Strategy

Timeout examples

-   Customer response
-   Payment pending
-   Approval pending

Timed-out workflows trigger escalation.

------------------------------------------------------------------------

# Workflow Events

Publish

-   WorkflowStarted
-   WorkflowPaused
-   WorkflowResumed
-   WorkflowCompleted
-   WorkflowFailed

------------------------------------------------------------------------

# Persistence

Persist

-   Workflow Definition
-   Workflow Instance
-   Current State
-   History
-   Variables

Support recovery after restart.

------------------------------------------------------------------------

# Versioning

Each workflow has a version.

Existing executions continue with their original version.

New executions use the latest version.

------------------------------------------------------------------------

# Observability

Capture

-   Running Workflows
-   Failed Workflows
-   Average Duration
-   Waiting Human Tasks
-   Retry Count

------------------------------------------------------------------------

# Security

Every workflow

-   Carries tenant context
-   Validates authorization
-   Audits user actions

------------------------------------------------------------------------

# Engineering Rules

Always

-   Keep workflows stateless
-   Keep business rules in domain
-   Publish workflow events
-   Support retries
-   Support compensation

Never

-   Access repositories directly
-   Place business rules inside workflow definitions
-   Depend on external SDKs

------------------------------------------------------------------------

# Workflow Checklist

-   Definition created
-   States defined
-   Events published
-   Retry configured
-   Timeout configured
-   Compensation implemented
-   Metrics added
-   Tests completed

------------------------------------------------------------------------

# Related Knowledge

-   02-layered-architecture.md
-   03-bounded-contexts.md
-   05-event-driven-architecture.md
-   07-ai-architecture.md

# End
