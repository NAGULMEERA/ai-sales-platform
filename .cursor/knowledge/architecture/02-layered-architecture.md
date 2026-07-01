# 02-layered-architecture.md

**Version:** 1.0\
**Status:** Approved Architecture

## Audience

-   Chief Architect
-   Solution Architect
-   Backend Engineers
-   AI Engineers
-   Workflow Engineers
-   Database Architects
-   Plugin Engineers
-   Security Engineers
-   QA Engineers
-   Code Reviewers
-   AI Coding Agents

------------------------------------------------------------------------

# 1. Purpose

This document defines the official layered architecture of the AI Sales
Employee Platform.

It is the authoritative reference for how every component within the
platform must be designed, implemented, tested, deployed, and evolved.

This document exists to ensure:

-   Consistent architecture
-   Maintainable code
-   High scalability
-   Strong separation of concerns
-   Production readiness
-   Long-term evolution

Every engineer, AI Agent, Skill, Prompt, Template, and Code Reviewer
must follow the architectural principles defined in this document.

This document complements the Enterprise Architecture and focuses
specifically on implementation boundaries.

------------------------------------------------------------------------

# 2. Why Layered Architecture?

Modern enterprise systems become difficult to maintain when
responsibilities are mixed.

Bad architecture example:

``` text
Controller
    ↓
Business Logic
    ↓
SQL
    ↓
AI Call
    ↓
Workflow
    ↓
Email
    ↓
Audit
    ↓
Response
```

Problems:

-   Tight coupling
-   Low testability
-   Duplicate logic
-   Slow development
-   High defect rate

------------------------------------------------------------------------

# 3. Architecture Vision

The platform is designed as an AI-Native Enterprise SaaS Platform
supporting:

-   Multi-Tenant SaaS
-   Multiple Industries
-   AI Agents
-   Workflow Automation
-   Event Driven Processing
-   Plugin Marketplace
-   Horizontal Scaling
-   Cloud Native Deployment
-   Future Microservices

------------------------------------------------------------------------

# 4. Architectural Philosophy

## Principle 1

Business drives technology.

## Principle 2

Each layer owns exactly one responsibility.

## Principle 3

Dependencies always point inward.

``` text
Infrastructure
      ↓
Application
      ↓
Domain
```

## Principle 4

External providers are replaceable.

## Principle 5

Business capabilities remain isolated.

## Principle 6

Every feature is production ready.

------------------------------------------------------------------------

# 5. Enterprise Layered Architecture

``` text
Client Applications
        │
        ▼
API Gateway
        │
        ▼
Presentation Layer
        │
        ▼
Application Layer
        │
 ┌──────┼───────────┐
 ▼      ▼           ▼
Workflow AI      Event Bus
        │
        ▼
Domain Layer
        │
        ▼
Infrastructure Layer
        │
        ▼
Data Layer
```

------------------------------------------------------------------------

# 6. Layer Responsibilities

## Presentation Layer

Responsibilities

-   REST APIs
-   Validation
-   DTO Mapping
-   Response Formatting

Must never contain:

-   Business Logic
-   SQL
-   Workflow Logic
-   AI Logic

------------------------------------------------------------------------

## Application Layer

Responsibilities

-   Use Cases
-   Transactions
-   Workflow Coordination
-   Event Publishing
-   AI Coordination
-   Plugin Coordination

Coordinates business use cases but does not own business rules.

------------------------------------------------------------------------

## Domain Layer

Contains:

-   Aggregates
-   Entities
-   Value Objects
-   Policies
-   Domain Services
-   Domain Events

The domain is the heart of the platform.

Never include:

-   SQL
-   HTTP
-   Kafka
-   Redis
-   Provider SDKs

------------------------------------------------------------------------

# 7. Layer Dependency Rules

Allowed

-   Presentation → Application
-   Application → Domain
-   Application → Workflow
-   Application → AI
-   Application → Plugin Contracts
-   Infrastructure → Domain Interfaces

Forbidden

-   Controller → Repository
-   Controller → Entity
-   Workflow → Repository
-   AI → Database
-   Plugin → Aggregate
-   Domain → Spring Framework
-   Domain → JPA
-   Domain → Kafka

------------------------------------------------------------------------

# 8. Architecture Decision

The platform is:

-   AI Native
-   Workflow Driven
-   Event Driven
-   Plugin Based
-   Multi-Tenant

Therefore the Domain remains the center of the architecture while AI,
workflows, events, and plugins extend business capabilities without
owning business rules.

------------------------------------------------------------------------

# End of Chapter 1


# 02-layered-architecture.md

# Chapter 2 - Layer Interaction & Communication

## 9. Layer Interaction Philosophy

Every layer communicates only with its immediate responsibility.

    Presentation
          │
          ▼
    Application
          │
          ▼
    Domain
          ▲
          │
    Infrastructure

Cross-layer shortcuts are forbidden because they increase coupling and
reduce maintainability.

------------------------------------------------------------------------

# 10. Request Lifecycle

    Client
       │
       ▼
    API Gateway
       │
       ▼
    Authentication
       │
       ▼
    Tenant Resolution
       │
       ▼
    Controller
       │
       ▼
    Application Service
       │
       ▼
    Domain Aggregate
       │
       ▼
    Repository
       │
       ▼
    Database

After persistence:

-   Publish Domain Events
-   Start Workflow (if required)
-   Trigger Notifications
-   Return Response

------------------------------------------------------------------------

# 11. Event Lifecycle

    Aggregate
        │
        ▼
    Domain Event
        │
        ▼
    Outbox
        │
        ▼
    Kafka
        │
        ▼
    Consumers

Rules

-   Events are immutable.
-   Events represent facts.
-   Events never contain business logic.
-   Consumers must be idempotent.

------------------------------------------------------------------------

# 12. Workflow Lifecycle

    Business Event
          │
          ▼
    Workflow Started
          │
          ▼
    State Transition
          │
          ▼
    AI Decision (optional)
          │
          ▼
    Human Task (optional)
          │
          ▼
    Completion

Workflow responsibilities

-   Coordinate activities
-   Retry failed tasks
-   Handle timeouts
-   Execute compensation
-   Publish completion events

Workflow must never own business rules.

------------------------------------------------------------------------

# 13. AI Invocation Lifecycle

    Application Service
          │
          ▼
    AI Engine
          │
          ▼
    Prompt Builder
          │
          ▼
    LLM Provider
          │
          ▼
    Structured Response
          │
          ▼
    Business Validation
          │
          ▼
    Application Service

Rules

-   Never trust raw LLM output.
-   Always validate structured output.
-   Keep prompts outside business logic.
-   Use provider abstraction.

------------------------------------------------------------------------

# 14. Plugin Execution Flow

    Application Service
          │
          ▼
    Plugin Contract
          │
          ▼
    Plugin Adapter
          │
          ▼
    External Provider

Examples

-   WhatsApp
-   Google Calendar
-   Twilio
-   Stripe
-   OpenAI

The platform depends only on plugin contracts.

------------------------------------------------------------------------

# 15. Layer Communication Rules

Preferred

-   Method Calls
-   Domain Events
-   Integration Events
-   Workflow Commands

Avoid

-   Shared mutable state
-   Direct database sharing
-   Static utility coupling
-   Circular dependencies

------------------------------------------------------------------------

# 16. Dependency Matrix

  Source           Allowed
  ---------------- ----------------------------------------
  Presentation     Application
  Application      Domain, Workflow, AI, Plugin Contracts
  Domain           Domain only
  Infrastructure   Domain Interfaces
  Plugins          External Providers

Forbidden

-   Controller → Repository
-   Controller → Entity
-   Workflow → Repository
-   AI → Database
-   Plugin → Aggregate

------------------------------------------------------------------------

# 17. Error Handling Strategy

Business Errors

-   Domain Exceptions
-   Validation Errors

Technical Errors

-   Database
-   Network
-   External Provider
-   Timeout

Recoverable Errors

-   Retry
-   Circuit Breaker
-   Fallback

------------------------------------------------------------------------

# 18. Engineering Checklist

-   Layer boundaries respected
-   Dependencies valid
-   No business logic in controllers
-   No provider SDK leakage
-   Domain remains framework independent
-   Events published correctly
-   Workflow used only for orchestration
-   AI output validated
-   Plugins accessed through contracts
-   Logging, metrics and tracing implemented

------------------------------------------------------------------------

# End of Chapter 2


# 02-layered-architecture.md

# Chapter 3 - Repository Structure, Modules & Cross-Cutting Architecture

## 19. Repository Structure Philosophy

Organize the repository around **business capabilities**, not technical
layers.

``` text
backend/
 ├── lead/
 ├── customer/
 ├── appointment/
 ├── workflow/
 ├── ai/
 ├── notification/
 └── common/
```

Every bounded context owns:

-   API
-   Application
-   Domain
-   Infrastructure
-   Workflow
-   Tests

------------------------------------------------------------------------

## 20. Maven Module Mapping

``` text
platform-parent
│
├── common
├── auth-service
├── tenant-service
├── lead-service
├── customer-service
├── workflow-engine
├── ai-engine
├── plugin-sdk
├── notification-service
└── analytics-service
```

Rules

-   Shared code belongs only in approved common modules.
-   Domain models are never shared between bounded contexts.
-   Communication occurs through APIs or events.

------------------------------------------------------------------------

## 21. Package Structure

Recommended package layout

``` text
lead
 ├── api
 ├── application
 ├── domain
 │    ├── aggregate
 │    ├── entity
 │    ├── valueobject
 │    ├── service
 │    └── event
 ├── infrastructure
 ├── workflow
 └── tests
```

Avoid package-by-layer across the whole application.

------------------------------------------------------------------------

## 22. Multi-Tenant Integration

Tenant context is resolved once and propagated throughout the request.

``` text
Gateway
   ↓
Security
   ↓
Tenant Resolver
   ↓
Application
   ↓
Repository
```

Rules

-   Every query is tenant-aware.
-   Every event contains tenant context.
-   Every workflow executes inside tenant context.

------------------------------------------------------------------------

## 23. Security Across Layers

Security responsibilities

Presentation

-   Authentication
-   Request validation

Application

-   Authorization
-   RBAC

Domain

-   Business authorization rules

Infrastructure

-   Secret management
-   Encryption
-   Audit persistence

Never bypass authorization.

------------------------------------------------------------------------

## 24. Observability Across Layers

Every layer must emit

-   Structured logs
-   Metrics
-   Traces
-   Correlation IDs

Business metrics examples

-   Leads Created
-   AI Qualification Success
-   Appointment Conversion
-   Workflow Completion Rate

------------------------------------------------------------------------

## 25. Transaction Boundaries

Transactions belong in the Application Layer.

``` text
Controller
   ↓
Application Service (@Transactional)
   ↓
Domain
   ↓
Repository
```

Do not open transactions in controllers or repositories.

------------------------------------------------------------------------

## 26. Caching Strategy

Use Redis only for

-   Reference data
-   Session state
-   Frequently accessed read models
-   Rate limiting

Never cache business decisions.

------------------------------------------------------------------------

## 27. Performance Strategy

Design for

-   Horizontal scaling
-   Stateless services
-   Async event processing
-   Pagination
-   Efficient indexes
-   Bulk operations

Avoid

-   N+1 queries
-   Large transactions
-   Chatty APIs
-   Blocking external calls

------------------------------------------------------------------------

## 28. Production Design Rules

Every feature must include

-   Configuration externalization
-   Health checks
-   Metrics
-   Tracing
-   Retry strategy
-   Timeout policy
-   Logging
-   Tests
-   Documentation

Production readiness is part of development.

------------------------------------------------------------------------

## 29. Architecture Review Checklist

Before implementation verify

-   Correct bounded context
-   Correct layer ownership
-   Valid dependency direction
-   Multi-tenant support
-   Security applied
-   Workflow required?
-   AI required?
-   Event required?
-   Plugin required?
-   Observability included
-   Tests included
-   Documentation updated

------------------------------------------------------------------------

# End of Chapter 3


# 02-layered-architecture.md

# Chapter 4 - Advanced Platform Integration & Production Architecture

## 30. Workflow Integration Strategy

The Workflow Engine orchestrates long-running business processes.

``` text
Lead Created
      │
      ▼
Qualification Workflow
      │
      ├── AI Qualification
      ├── Property Matching
      ├── Appointment Scheduling
      └── Customer Follow-up
```

Workflow responsibilities

-   Orchestration
-   Retry
-   Timeout
-   Compensation
-   Escalation
-   Human Tasks

Never place business rules inside workflows.

------------------------------------------------------------------------

## 31. AI Layer Integration

AI is a platform capability, not a business owner.

``` text
Application Service
        │
        ▼
AI Engine
        │
        ▼
Prompt Registry
        │
        ▼
Model Provider
        │
        ▼
Structured Response
        │
        ▼
Business Validation
```

Rules

-   Prompts are versioned.
-   AI returns structured responses.
-   Business validates AI output.
-   AI never updates the database directly.

------------------------------------------------------------------------

## 32. Event Driven Architecture

Events decouple bounded contexts.

``` text
Lead Aggregate
      │
      ▼
LeadCreated Event
      │
      ▼
Kafka
      │
 ┌────┼─────────────┐
 ▼    ▼             ▼
Workflow Notification Analytics
```

Rules

-   Immutable events
-   Versioned schemas
-   Idempotent consumers
-   Retry with DLQ
-   Outbox pattern for publishing

------------------------------------------------------------------------

## 33. Plugin Architecture

External providers are accessed through plugins.

``` text
Application
     │
     ▼
Plugin Contract
     │
     ▼
Provider Adapter
     │
     ▼
External SDK
```

Examples

-   WhatsApp
-   Email
-   SMS
-   Voice
-   Calendar
-   Payment
-   AI Providers

The core platform never depends directly on provider SDKs.

------------------------------------------------------------------------

## 34. Resilience Strategy

Every integration should support

-   Retry
-   Timeout
-   Circuit Breaker
-   Fallback
-   Bulkhead
-   Rate Limiting

Failure of one provider must not stop the platform.

------------------------------------------------------------------------

## 35. Production Deployment Principles

Services should be

-   Stateless
-   Horizontally scalable
-   Containerized
-   Observable
-   Secure
-   Configuration driven

Use

-   Docker
-   Kubernetes
-   Health Checks
-   Readiness Probes
-   Liveness Probes

------------------------------------------------------------------------

## 36. Scalability Guidelines

Design for

-   Horizontal scaling
-   Event-driven processing
-   Async workflows
-   Read/write separation
-   Redis caching
-   Database indexing

Avoid

-   Shared session state
-   Synchronous external dependencies
-   Long running HTTP requests

------------------------------------------------------------------------

## 37. Architecture Decision Matrix

  Requirement                   Preferred Solution
  ----------------------------- ---------------------
  Business Rule                 Domain
  Use Case                      Application Service
  Long Process                  Workflow
  AI Decision                   AI Engine
  External Integration          Plugin
  Cross Context Communication   Events
  Persistence                   Repository

------------------------------------------------------------------------

## 38. Final Architecture Principles

Always

-   Keep the domain independent.
-   Isolate providers behind plugins.
-   Coordinate through application services.
-   Orchestrate using workflows.
-   Communicate across contexts using events.
-   Validate AI output.
-   Design for observability.
-   Design for production.

Never

-   Place business logic in controllers.
-   Allow plugins to modify domain rules.
-   Access repositories from workflows.
-   Couple the domain to infrastructure.
-   Trust raw AI responses.

------------------------------------------------------------------------

# End of Chapter 4


# 02-layered-architecture.md

# Chapter 5 -- Security, Observability & Operational Excellence

## 39. Security Architecture

Security is a first-class architectural concern.

Every request entering the platform must pass through security
validation before reaching business logic.

### Security Layers

``` text
Client
    │
    ▼
API Gateway
    │
    ▼
Authentication
    │
    ▼
Authorization
    │
    ▼
Tenant Validation
    │
    ▼
Application Service
    │
    ▼
Domain
```

### Responsibilities

### Presentation Layer

-   Authentication
-   Input Validation
-   Request Sanitization

### Application Layer

-   Authorization
-   RBAC
-   Tenant Validation

### Domain Layer

-   Business Authorization
-   Domain Policies

### Infrastructure Layer

-   Encryption
-   Secret Management
-   Audit Persistence

------------------------------------------------------------------------

## 40. Authentication Strategy

Supported mechanisms

-   JWT
-   OAuth2
-   API Keys
-   Service-to-Service Tokens

Rules

-   Never trust client identity.
-   Validate every token.
-   Tokens must contain tenant information.
-   Expired tokens are rejected immediately.

------------------------------------------------------------------------

## 41. Authorization Strategy

Authorization follows Role-Based Access Control (RBAC).

Example roles

-   Super Admin
-   Tenant Admin
-   Sales Manager
-   Sales Agent
-   AI Agent
-   Read Only User

Future roadmap: ABAC.

------------------------------------------------------------------------

## 42. Tenant Isolation

Every request must contain:

-   Tenant ID
-   Organization ID
-   User ID

Every query, workflow and event must remain tenant aware.

Forbidden:

-   Cross-tenant joins
-   Shared mutable tenant state
-   Shared caches without tenant keys

------------------------------------------------------------------------

## 43. Secret Management

Never hardcode:

-   Database Passwords
-   JWT Secrets
-   API Keys
-   AI Provider Keys
-   Twilio Credentials

Use external secret management.

------------------------------------------------------------------------

## 44. Audit Strategy

Capture

-   User
-   Tenant
-   Action
-   Timestamp
-   Correlation ID
-   Before State
-   After State

Audit logs are immutable.

------------------------------------------------------------------------

## 45. Observability Philosophy

Three pillars

-   Logs
-   Metrics
-   Traces

Every business capability must be observable.

------------------------------------------------------------------------

## 46. Logging Standards

Structured logging only.

Include

-   Correlation ID
-   Tenant ID
-   Request ID
-   Workflow ID

Never log secrets or personal data.

------------------------------------------------------------------------

## 47. Metrics Strategy

Technical

-   Request Count
-   Latency
-   Error Rate
-   CPU
-   Memory

Business

-   Leads Created
-   AI Qualification Rate
-   Appointment Conversion
-   Revenue

------------------------------------------------------------------------

## 48. Distributed Tracing

``` text
Gateway
   ↓
API
   ↓
Workflow
   ↓
AI
   ↓
Plugin
   ↓
Database
```

Every request must have a Correlation ID.

------------------------------------------------------------------------

## 49. Health Checks

Implement

-   Liveness
-   Readiness
-   Startup

Validate

-   PostgreSQL
-   Redis
-   Kafka
-   AI Provider
-   Plugin Registry

------------------------------------------------------------------------

## 50. AI Observability

Capture

-   Prompt Version
-   Model
-   Token Usage
-   Latency
-   Cost
-   Tool Calls
-   Errors

------------------------------------------------------------------------

## 51. Workflow Observability

Track

-   Started
-   Running
-   Waiting
-   Failed
-   Completed
-   Escalated

------------------------------------------------------------------------

## 52. Event Observability

Track

-   Published Events
-   Consumed Events
-   Retry Count
-   DLQ Messages
-   Replay Count

------------------------------------------------------------------------

## 53. Operational Runbooks

Each capability must define

-   Symptoms
-   Alerts
-   Dashboards
-   Recovery Steps
-   Rollback

------------------------------------------------------------------------

## 54. Production Readiness Checklist

-   Authentication verified
-   Authorization verified
-   Tenant isolation tested
-   Secrets externalized
-   Audit enabled
-   Structured logging implemented
-   Metrics published
-   Tracing enabled
-   Health checks passing
-   Alerts configured
-   Runbooks documented

------------------------------------------------------------------------

# End of Chapter 5


# 02-layered-architecture.md

# Chapter 6 -- Engineering Standards, Decision Matrix & Production Readiness

## 55. Architecture Decision Matrix

  Requirement                   Recommended Layer
  ----------------------------- ---------------------
  Business Rule                 Domain
  Use Case                      Application Service
  Long Running Process          Workflow
  AI Capability                 AI Engine
  External Integration          Plugin
  Cross Context Communication   Events
  Data Persistence              Repository

Always choose the simplest architectural solution that satisfies the
business requirement.

------------------------------------------------------------------------

## 56. Best Practices

Always

-   Keep the Domain independent.
-   Design by bounded context.
-   Use feature-based packages.
-   Publish domain events.
-   Validate AI responses.
-   Hide providers behind plugins.
-   Externalize configuration.
-   Keep services stateless.
-   Write automated tests.
-   Add observability from day one.

------------------------------------------------------------------------

## 57. Anti-Patterns

Avoid

-   Fat Controllers
-   God Services
-   Business Logic in Controllers
-   Business Logic in Workflows
-   Direct SDK Usage
-   Shared Databases Across Contexts
-   Circular Dependencies
-   N+1 Queries
-   Static Utility Classes
-   Hardcoded Configuration
-   Missing Tenant Context

Every anti-pattern increases long-term maintenance cost.

------------------------------------------------------------------------

## 58. Code Review Guidelines

Every implementation should verify

-   Layer ownership
-   Dependency direction
-   DDD compliance
-   Repository reuse
-   Security
-   Observability
-   Test coverage
-   Performance
-   Documentation

No implementation is complete until reviewed.

------------------------------------------------------------------------

## 59. Testing Strategy

Unit Tests

-   Domain
-   Value Objects
-   Policies

Integration Tests

-   Repository
-   Database
-   Workflow
-   AI
-   Plugins

Contract Tests

-   REST APIs
-   Events
-   Plugin Contracts

End-to-End Tests

-   Lead Journey
-   Appointment Journey
-   Billing Journey

------------------------------------------------------------------------

## 60. Production Readiness Standards

Every deployment must provide

-   Health Checks
-   Metrics
-   Logs
-   Traces
-   Retry Policies
-   Timeouts
-   Circuit Breakers
-   Feature Flags
-   Configuration Management
-   Rollback Strategy

Production readiness is part of development.

------------------------------------------------------------------------

## 61. Architecture Review Checklist

Before merging code confirm

-   Correct bounded context
-   Correct layer
-   No architecture violations
-   Tenant awareness
-   Security validation
-   AI validation
-   Workflow review
-   Event review
-   Plugin review
-   Observability complete
-   Tests passing
-   Documentation updated

------------------------------------------------------------------------

## 62. Related Knowledge

Architecture

-   01-platform-overview.md
-   03-bounded-contexts.md
-   04-plugin-architecture.md
-   05-event-driven-architecture.md
-   06-workflow-engine.md
-   07-ai-architecture.md
-   08-security-architecture.md
-   09-observability-architecture.md
-   10-deployment-architecture.md

Rules

-   .cursor/rules/

Agents

-   Chief Architect
-   Solution Architect
-   Backend Engineer
-   Database Architect
-   Workflow Engineer
-   AI Engineer
-   Security Engineer
-   Plugin Engineer
-   Observability Engineer
-   QA Engineer
-   Code Reviewer

------------------------------------------------------------------------

## 63. Final Architecture Principles

Business drives architecture.

Domain owns business rules.

Application coordinates use cases.

Workflows orchestrate long-running processes.

AI augments decision making.

Plugins isolate external providers.

Events integrate bounded contexts.

Infrastructure supports the domain.

Observability is mandatory.

Security is built in.

Production readiness is the default.

------------------------------------------------------------------------

# End of Chapter 6

This completes the Layered Architecture handbook foundation.
