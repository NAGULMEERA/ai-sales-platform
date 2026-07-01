# 07-testing-standards.md

# Testing Engineering Standards

Version: 1.0

## Purpose

Defines testing standards for the AI Sales Employee Platform to ensure
correctness, reliability, maintainability, and production confidence.

------------------------------------------------------------------------

# Testing Philosophy

-   Test business behavior, not implementation details.
-   Automate all repeatable tests.
-   Every bug should result in a regression test.
-   Fast feedback is essential.

------------------------------------------------------------------------

# Testing Pyramid

``` text
        E2E
      Integration
     Unit Tests
```

Prefer many unit tests, fewer integration tests, and minimal end-to-end
tests.

------------------------------------------------------------------------

# Unit Testing

Frameworks

-   JUnit 5
-   Mockito
-   AssertJ

Rules

-   Test one behavior per test
-   Use Arrange → Act → Assert
-   Mock external dependencies
-   Avoid Spring context

------------------------------------------------------------------------

# Integration Testing

Use

-   Spring Boot Test
-   Testcontainers
-   Real PostgreSQL
-   Real Redis (where applicable)

Test

-   Repositories
-   Transactions
-   Event publishing
-   Configuration

------------------------------------------------------------------------

# Contract Testing

Verify

-   REST APIs
-   Kafka events
-   Plugin contracts
-   External integrations

Prevent breaking consumer contracts.

------------------------------------------------------------------------

# End-to-End Testing

Validate complete user journeys.

Examples

-   Lead Capture → Appointment
-   Customer Onboarding
-   Subscription Purchase
-   AI Qualification Workflow

------------------------------------------------------------------------

# Performance Testing

Measure

-   API latency
-   Throughput
-   Database performance
-   Workflow execution
-   AI response times

Use representative production workloads.

------------------------------------------------------------------------

# Test Data Management

-   Use builders/factories
-   Isolate test data
-   Clean data after execution
-   Avoid shared mutable fixtures

------------------------------------------------------------------------

# Coverage Expectations

Target

-   Domain Layer: High coverage
-   Application Layer: High coverage
-   Infrastructure: Risk-based coverage

Focus on critical business logic over percentage alone.

------------------------------------------------------------------------

# CI Quality Gates

Require

-   Unit tests passing
-   Integration tests passing
-   Static analysis passing
-   Security scan passing
-   Coverage threshold met

------------------------------------------------------------------------

# Review Checklist

-   Business rules tested
-   Edge cases covered
-   Negative scenarios included
-   Test names meaningful
-   No flaky tests
-   Fast execution

------------------------------------------------------------------------

# Related Knowledge

-   01-java-standards.md
-   02-spring-boot-standards.md
-   03-ddd-standards.md

# End
