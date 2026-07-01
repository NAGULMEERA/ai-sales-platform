# 05-appointment-lifecycle.md

# Appointment Lifecycle

Version: 1.0

## Purpose

Defines the complete lifecycle for customer appointments, site visits,
meetings, and follow-up activities within the AI Sales Employee
Platform.

------------------------------------------------------------------------

# Business Objective

Ensure every qualified customer progresses smoothly from scheduling
through completion while maximizing attendance, customer satisfaction,
and conversion.

------------------------------------------------------------------------

# Appointment Sources

Appointments may originate from:

-   AI Recommendation
-   Sales Executive
-   Customer Self-Service
-   Website
-   WhatsApp
-   Phone Call
-   Workflow Automation

------------------------------------------------------------------------

# Appointment Lifecycle

``` text
Requested
    │
    ▼
Validated
    │
    ▼
Scheduled
    │
    ▼
Confirmed
    │
    ▼
Reminder Sent
    │
    ▼
In Progress
    │
    ▼
Completed
    │
 ┌──┴──────────┐
 ▼             ▼
Won          Follow-up
```

Alternative states

-   Rescheduled
-   Cancelled
-   No Show

------------------------------------------------------------------------

# Calendar Integration

Supported providers

-   Google Calendar
-   Microsoft 365
-   Internal Scheduler

Calendar synchronization must remain bidirectional.

------------------------------------------------------------------------

# AI Assistance

AI may assist with

-   Best meeting time
-   Property recommendations
-   Route suggestions
-   Follow-up summaries
-   Next Best Action

Business validation remains mandatory.

------------------------------------------------------------------------

# Notifications

Automatically send

-   Confirmation
-   Reminder
-   Reschedule Notice
-   Cancellation Notice
-   Follow-up Message

Supported channels

-   WhatsApp
-   Email
-   SMS
-   Push Notification

------------------------------------------------------------------------

# Workflow Integration

Workflow coordinates

-   Scheduling
-   Approval
-   Reminder
-   Escalation
-   Follow-up

Business rules remain in the domain layer.

------------------------------------------------------------------------

# Events

Publish

-   AppointmentRequested
-   AppointmentScheduled
-   AppointmentConfirmed
-   ReminderSent
-   AppointmentCompleted
-   AppointmentCancelled
-   AppointmentRescheduled

------------------------------------------------------------------------

# Business KPIs

Track

-   Scheduling Time
-   Confirmation Rate
-   Attendance Rate
-   No Show Rate
-   Completion Rate
-   Conversion After Appointment

------------------------------------------------------------------------

# Engineering Guidelines

Always

-   Preserve tenant context
-   Synchronize calendars
-   Publish events
-   Audit changes
-   Support rescheduling

Never

-   Double-book appointments
-   Skip reminders
-   Bypass workflow validation

------------------------------------------------------------------------

# Related Knowledge

-   02-layered-architecture.md
-   03-customer-lifecycle.md
-   05-event-driven-architecture.md
-   06-workflow-engine.md
-   07-ai-architecture.md

# End
