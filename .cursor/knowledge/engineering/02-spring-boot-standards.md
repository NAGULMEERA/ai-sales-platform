# 02-spring-boot-standards.md

# Spring Boot Engineering Standards

Version: 1.0

## Purpose

Defines Spring Boot development standards for the AI Sales Employee
Platform to ensure consistency, scalability, and production readiness.

------------------------------------------------------------------------

# Spring Boot Version

-   Spring Boot 4.x
-   Java 21+
-   Maven multi-module
-   application.properties preferred

------------------------------------------------------------------------

# Project Structure

``` text
feature/
 ├── api
 ├── application
 ├── domain
 ├── infrastructure
 └── config
```

Organize by bounded context, not technical layer alone.

------------------------------------------------------------------------

# Dependency Injection

Use

-   Constructor injection
-   final fields
-   Interfaces for abstractions

Avoid

-   Field injection
-   Manual object creation with `new`

------------------------------------------------------------------------

# REST Controllers

Controllers should

-   Validate input
-   Delegate to application services
-   Return DTOs
-   Never contain business rules

------------------------------------------------------------------------

# Application Services

Responsibilities

-   Coordinate use cases
-   Manage transactions
-   Publish domain events
-   Invoke workflows and plugins

------------------------------------------------------------------------

# Transactions

-   Keep transactions short
-   Annotate application services
-   Never start transactions in controllers
-   Avoid long-running transactions

------------------------------------------------------------------------

# Validation

Use Bean Validation

-   @Valid
-   @NotNull
-   @NotBlank
-   Custom validators for business constraints

------------------------------------------------------------------------

# Configuration

Externalize

-   URLs
-   Secrets
-   Feature flags
-   Timeouts

Never hardcode environment values.

------------------------------------------------------------------------

# Exception Handling

Use

-   @ControllerAdvice
-   RFC7807 Problem Details
-   Domain-specific exceptions

Never expose stack traces.

------------------------------------------------------------------------

# Persistence

-   Spring Data JPA
-   Flyway for schema changes
-   Optimistic locking
-   Repository per aggregate

------------------------------------------------------------------------

# Observability

Enable

-   Spring Boot Actuator
-   Micrometer
-   OpenTelemetry
-   Structured logging

------------------------------------------------------------------------

# Security

-   Spring Security
-   JWT/OAuth2
-   RBAC
-   Tenant validation
-   CSRF where applicable

------------------------------------------------------------------------

# Resilience

Use

-   Resilience4j
-   Retry
-   Circuit Breaker
-   Timeout
-   Bulkhead

------------------------------------------------------------------------

# Testing

Implement

-   Unit tests
-   Integration tests
-   Slice tests
-   Testcontainers

------------------------------------------------------------------------

# Engineering Checklist

-   Constructor injection
-   DTO validation
-   Transactions correct
-   Health checks enabled
-   Metrics exported
-   Tests passing
-   Documentation updated

------------------------------------------------------------------------

# Related Knowledge

-   01-java-standards.md
-   02-layered-architecture.md
-   08-security-architecture.md
-   09-observability-architecture.md

# End
