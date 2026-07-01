# 04-appointment-domain.md

# Appointment Domain

Version: 1.0

## Purpose

Defines the Appointment bounded context using Domain-Driven Design
(DDD). This context manages appointment scheduling, site visits,
calendar coordination, and attendance while enforcing business rules and
tenant isolation.

------------------------------------------------------------------------

# Aggregate

**Appointment** is the aggregate root.

Responsibilities

-   Manage appointment lifecycle
-   Validate scheduling rules
-   Coordinate attendees
-   Publish appointment events

------------------------------------------------------------------------

# Entities

-   Appointment (Aggregate Root)
-   Attendee
-   Schedule
-   Reminder
-   MeetingLocation
-   VisitRecord

------------------------------------------------------------------------

# Value Objects

-   AppointmentId
-   TimeSlot
-   Duration
-   Location
-   CalendarReference
-   AppointmentStatus

Value objects are immutable.

------------------------------------------------------------------------

# Aggregate Invariants

An Appointment:

-   Must belong to exactly one Tenant
-   Must have at least one attendee
-   Cannot overlap another confirmed appointment for the same resource
-   Cannot be completed before it is confirmed
-   Cannot be modified after archival

------------------------------------------------------------------------

# Domain Services

-   AppointmentSchedulingService
-   AvailabilityService
-   CalendarSynchronizationService
-   ReminderPolicyService

------------------------------------------------------------------------

# Repository

``` text
AppointmentRepository
```

Responsibilities

-   Save Aggregate
-   Find by Id
-   Find by Tenant
-   Find by Customer
-   Find by Date Range
-   Detect scheduling conflicts

------------------------------------------------------------------------

# Domain Events

-   AppointmentRequested
-   AppointmentScheduled
-   AppointmentConfirmed
-   AppointmentRescheduled
-   ReminderSent
-   AppointmentCompleted
-   AppointmentCancelled
-   NoShowRecorded

Events are immutable business facts.

------------------------------------------------------------------------

# State Model

``` text
Requested
    │
Validated
    │
Scheduled
    │
Confirmed
    │
In Progress
    │
Completed
```

Alternative states

-   Rescheduled
-   Cancelled
-   No Show
-   Archived

------------------------------------------------------------------------

# Business Rules

-   Double booking is prohibited.
-   Calendar synchronization must remain consistent.
-   Reminders follow configurable policies.
-   Every appointment change is audited.
-   Tenant isolation is mandatory.

------------------------------------------------------------------------

# Engineering Guidelines

Always

-   Validate availability before scheduling
-   Publish domain events
-   Preserve appointment history
-   Use immutable value objects
-   Audit schedule changes

Never

-   Modify appointments directly in repositories
-   Skip conflict detection
-   Bypass workflow validation
-   Share appointments across tenants

------------------------------------------------------------------------

# Related Knowledge

-   02-layered-architecture.md
-   03-bounded-contexts.md
-   05-appointment-lifecycle.md
-   05-event-driven-architecture.md
-   06-workflow-engine.md

# End
