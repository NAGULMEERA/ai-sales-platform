# 01-java-standards.md

# Java Engineering Standards

Version: 1.0

## Purpose

Defines Java coding standards for the AI Sales Employee Platform. Every
Java module must follow these conventions to ensure consistency,
maintainability, and production readiness.

------------------------------------------------------------------------

# Java Version

-   Java 21+ (LTS minimum)
-   Prefer modern language features
-   Avoid deprecated APIs

------------------------------------------------------------------------

# Package Structure

``` text
com.company.platform
 ├── application
 ├── domain
 ├── infrastructure
 ├── api
 ├── config
 └── shared
```

Organize by bounded context first, then by layer.

------------------------------------------------------------------------

# Naming Standards

Classes

-   PascalCase
-   Nouns for domain objects
-   Verbs for services ending with `Service`

Methods

-   camelCase
-   Express business intent

Constants

-   UPPER_SNAKE_CASE

Packages

-   lowercase only

------------------------------------------------------------------------

# Class Design

Prefer

-   Small cohesive classes
-   Constructor injection
-   Final fields
-   Interfaces for contracts
-   Composition over inheritance

Avoid

-   God classes
-   Static mutable state
-   Deep inheritance trees

------------------------------------------------------------------------

# Domain Model Rules

-   Business rules belong in aggregates.
-   Value objects are immutable.
-   Entities expose behavior, not setters.
-   Publish domain events from aggregates.

------------------------------------------------------------------------

# Exception Handling

Create domain-specific exceptions.

Never

-   Swallow exceptions
-   Catch `Exception`
-   Return `null` for failures

Use meaningful messages and preserve root causes.

------------------------------------------------------------------------

# Collections

Prefer

-   List
-   Set
-   Map

Return empty collections instead of `null`.

Use streams when they improve readability.

------------------------------------------------------------------------

# Concurrency

Use

-   CompletableFuture
-   Virtual Threads (when appropriate)
-   ExecutorService abstraction

Avoid manual thread creation.

------------------------------------------------------------------------

# Logging

Use SLF4J.

Always include

-   Correlation ID
-   Tenant ID
-   Business identifiers

Never log secrets.

------------------------------------------------------------------------

# Testing

Follow Arrange → Act → Assert.

Target

-   High domain coverage
-   Deterministic tests
-   No shared mutable fixtures

------------------------------------------------------------------------

# Code Review Checklist

-   Single Responsibility
-   Clean naming
-   No duplicated logic
-   Proper validation
-   Secure coding
-   Unit tests included

------------------------------------------------------------------------

# Related Knowledge

-   02-layered-architecture.md
-   10-common-domain.md
-   02-spring-boot-standards.md

# End
