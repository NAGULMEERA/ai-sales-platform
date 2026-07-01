# 10-code-review-checklist.md

# Code Review Checklist

Version: 1.0

## Purpose

Defines the mandatory review checklist before any code is merged into
the AI Sales Employee Platform.

------------------------------------------------------------------------

# Architecture Review

-   Correct bounded context
-   Correct layer
-   No circular dependencies
-   Dependency direction respected
-   Feature follows platform architecture

------------------------------------------------------------------------

# Domain Review

-   Aggregate invariants enforced
-   Business rules in domain
-   Value objects immutable
-   Domain events published
-   Repository contains no business logic

------------------------------------------------------------------------

# Spring Boot Review

-   Constructor injection
-   Transactions at application layer
-   Validation implemented
-   Configuration externalized
-   Controllers remain thin

------------------------------------------------------------------------

# API Review

-   REST conventions followed
-   DTOs used
-   Bean Validation enabled
-   RFC7807 Problem Details
-   OpenAPI updated
-   API version respected

------------------------------------------------------------------------

# Database Review

-   Flyway migration included
-   Naming standards followed
-   Indexes reviewed
-   Tenant isolation enforced
-   Optimistic locking used

------------------------------------------------------------------------

# Event Review

-   Correct event type
-   Schema versioned
-   Outbox pattern used
-   Consumer idempotent
-   Retry and DLQ configured

------------------------------------------------------------------------

# Security Review

-   Authentication verified
-   Authorization verified
-   Secrets externalized
-   Input validation complete
-   Sensitive data protected

------------------------------------------------------------------------

# AI Review

-   Prompt versioned
-   Structured output validated
-   Guardrails enabled
-   Tool access controlled
-   AI interactions audited

------------------------------------------------------------------------

# Workflow Review

-   State transitions valid
-   Compensation defined
-   Timeout configured
-   Human tasks audited
-   Workflow events published

------------------------------------------------------------------------

# Observability Review

-   Structured logs
-   Metrics exported
-   Traces enabled
-   Health checks implemented
-   Dashboards and alerts updated

------------------------------------------------------------------------

# Testing Review

-   Unit tests
-   Integration tests
-   Contract tests
-   Critical E2E scenarios
-   CI pipeline passing

------------------------------------------------------------------------

# Production Readiness

-   Feature flags considered
-   Rollback documented
-   Performance reviewed
-   Documentation updated
-   Monitoring verified

------------------------------------------------------------------------

# Merge Checklist

-   Architecture approved
-   Code reviewed
-   Static analysis clean
-   Security scan passed
-   Tests passed
-   Ready for deployment

------------------------------------------------------------------------

# Related Knowledge

-   01-java-standards.md
-   02-spring-boot-standards.md
-   03-ddd-standards.md
-   04-rest-api-standards.md
-   05-database-standards.md
-   06-event-standards.md
-   07-testing-standards.md
-   08-security-standards.md
-   09-observability-standards.md

# End
