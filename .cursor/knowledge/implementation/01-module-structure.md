# 01-module-structure.md

# Module Structure Standards

Version: 1.0

## Purpose

Defines the standard Maven module structure for the AI Sales Employee
Platform.

------------------------------------------------------------------------

# Architecture Principles

-   Modular Monolith first
-   Clear bounded contexts
-   Independent modules
-   No cyclic dependencies

------------------------------------------------------------------------

# Root Layout

``` text
platform/
├── platform-api
├── platform-application
├── platform-domain
├── platform-infrastructure
├── platform-shared
├── plugins/
├── workflows/
└── ai/
```

------------------------------------------------------------------------

# Feature Layout

``` text
lead/
├── api/
├── application/
├── domain/
├── infrastructure/
└── config/
```

------------------------------------------------------------------------

# Layer Responsibilities

## API

-   Controllers
-   DTOs
-   Validation

## Application

-   Use Cases
-   Transactions
-   Event publishing

## Domain

-   Aggregates
-   Entities
-   Value Objects
-   Domain Services

## Infrastructure

-   JPA
-   Kafka
-   Redis
-   Plugin adapters

------------------------------------------------------------------------

# Dependency Rules

``` text
API
 ↓
Application
 ↓
Domain
 ↑
Infrastructure
```

Domain never depends on Spring Boot or databases.

------------------------------------------------------------------------

# Shared Module

Contains only reusable cross-cutting components:

-   Tenant Context
-   Correlation Context
-   Base Domain Types
-   Exceptions
-   Utilities (non-business)

------------------------------------------------------------------------

# Naming

Modules

-   feature-name

Packages

-   lowercase

Artifacts

-   platform-feature

------------------------------------------------------------------------

# Review Checklist

-   Correct module
-   Correct package
-   No cyclic dependency
-   Domain isolated
-   Feature organized by bounded context

------------------------------------------------------------------------

# Related Knowledge

-   02-layered-architecture.md
-   03-bounded-contexts.md
-   01-java-standards.md

# End
