# 09-workflow-domain.md

# Workflow Domain

Version: 1.0

## Purpose

Defines the Workflow bounded context using Domain-Driven Design (DDD).
This context models long-running business processes, state transitions,
human tasks, timers, and compensation logic.

------------------------------------------------------------------------

# Aggregate

**WorkflowInstance** is the aggregate root.

Responsibilities

-   Manage workflow execution
-   Enforce valid state transitions
-   Coordinate tasks
-   Publish workflow events

------------------------------------------------------------------------

# Entities

-   WorkflowInstance (Aggregate Root)
-   WorkflowDefinition
-   WorkflowTask
-   HumanTask
-   Timer
-   CompensationAction
-   WorkflowVariable

------------------------------------------------------------------------

# Value Objects

-   WorkflowId
-   WorkflowVersion
-   State
-   Transition
-   TaskStatus
-   DueDate

Value objects are immutable.

------------------------------------------------------------------------

# Aggregate Invariants

A WorkflowInstance:

-   Must belong to one Tenant
-   Must reference one WorkflowDefinition version
-   Cannot execute invalid transitions
-   Cannot complete with pending mandatory tasks
-   Must preserve execution history

------------------------------------------------------------------------

# Domain Services

-   WorkflowExecutionService
-   StateTransitionService
-   EscalationService
-   CompensationService
-   TimeoutService

------------------------------------------------------------------------

# Repository

``` text
WorkflowRepository
WorkflowDefinitionRepository
```

Responsibilities

-   Save workflow instances
-   Load workflow definitions
-   Query active workflows
-   Query waiting human tasks

------------------------------------------------------------------------

# Domain Events

-   WorkflowStarted
-   TaskCreated
-   HumanTaskAssigned
-   TimerExpired
-   WorkflowPaused
-   WorkflowResumed
-   WorkflowCompleted
-   WorkflowFailed

------------------------------------------------------------------------

# State Model

``` text
Created
   │
Running
   │
Waiting
   │
Resumed
   │
Completed
```

Alternative states

-   Failed
-   Cancelled
-   Compensated

------------------------------------------------------------------------

# Business Rules

-   State transitions are validated.
-   Human approvals are auditable.
-   Compensation follows reverse execution order.
-   Workflow definitions are versioned.
-   Tenant isolation is mandatory.

------------------------------------------------------------------------

# Engineering Guidelines

Always

-   Publish workflow events
-   Validate transitions
-   Preserve execution history
-   Audit human tasks
-   Use immutable value objects

Never

-   Embed business rules in workflows
-   Modify completed workflows
-   Access repositories from task handlers directly
-   Bypass compensation logic

------------------------------------------------------------------------

# Related Knowledge

-   02-layered-architecture.md
-   05-event-driven-architecture.md
-   06-workflow-engine.md
-   08-ai-domain.md
-   10-common-domain.md

# End
