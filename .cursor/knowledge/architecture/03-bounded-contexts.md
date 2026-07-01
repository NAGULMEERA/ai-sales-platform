# 03-bounded-contexts.md

# Bounded Contexts

Version: 1.0

## Purpose

This document defines the bounded contexts of the AI Sales Employee
Platform and establishes ownership boundaries.

------------------------------------------------------------------------

# Architecture Philosophy

The platform is organized by business capabilities, not technical
layers.

Each bounded context owns:

-   APIs
-   Application Services
-   Domain Model
-   Aggregates
-   Repositories
-   Events
-   Workflows
-   Tests

No bounded context directly modifies another bounded context's internal
state.

------------------------------------------------------------------------

# Core Bounded Contexts

## Lead

Owns

-   Lead Aggregate
-   Lead Qualification
-   Lead Source
-   Lead Assignment
-   Lead Status

Publishes

-   LeadCreated
-   LeadQualified
-   LeadAssigned

Consumes

-   CustomerCreated
-   AppointmentScheduled

------------------------------------------------------------------------

## Customer

Owns

-   Customer Aggregate
-   Customer Profile
-   Contact Information
-   Preferences

Publishes

-   CustomerCreated
-   CustomerUpdated

------------------------------------------------------------------------

## Conversation

Owns

-   Chat History
-   AI Conversations
-   WhatsApp Sessions
-   Voice Sessions

------------------------------------------------------------------------

## Appointment

Owns

-   Site Visits
-   Meetings
-   Calendar Integration
-   Scheduling Rules

Publishes

-   AppointmentScheduled
-   AppointmentCompleted

------------------------------------------------------------------------

## Workflow

Owns

-   Workflow Definitions
-   Workflow Execution
-   State Machines
-   Human Tasks

------------------------------------------------------------------------

## AI

Owns

-   Prompt Registry
-   Tool Calling
-   RAG
-   AI Memory
-   Structured Output

------------------------------------------------------------------------

## Plugin

Owns

-   Provider Registry
-   Plugin Contracts
-   Provider Adapters
-   Marketplace Extensions

------------------------------------------------------------------------

## Billing

Owns

-   Plans
-   Subscriptions
-   Invoices
-   Payments

------------------------------------------------------------------------

## Notification

Owns

-   Email
-   SMS
-   WhatsApp
-   Push Notifications

------------------------------------------------------------------------

## Analytics

Owns

-   Dashboards
-   KPIs
-   Reports
-   AI Metrics

------------------------------------------------------------------------

# Context Communication

Preferred mechanisms

-   Domain Events
-   Integration Events
-   Public APIs

Never

-   Share database tables
-   Share aggregates
-   Bypass public contracts

------------------------------------------------------------------------

# Ownership Rules

Each bounded context owns

-   Business Rules
-   Persistence
-   Validation
-   Events

Other contexts interact only through contracts.

------------------------------------------------------------------------

# Anti-Patterns

Avoid

-   Shared Database
-   Shared Business Logic
-   Cross Context Transactions
-   Circular Dependencies
-   Generic Utility Domains

------------------------------------------------------------------------

# Engineering Checklist

Before creating a feature verify

-   Correct bounded context selected
-   Aggregate ownership respected
-   Events defined
-   Public contracts identified
-   Workflow required?
-   AI required?
-   Plugin required?
-   Tests added
-   Documentation updated

------------------------------------------------------------------------

# Related Knowledge

-   01-platform-overview.md
-   02-layered-architecture.md
-   04-plugin-architecture.md
-   05-event-driven-architecture.md

# End
