# 02-package-conventions.md

# Package Conventions

Version: 1.0

## Purpose

Defines the standard package organization for every bounded context in
the AI Sales Employee Platform.

------------------------------------------------------------------------

# Principles

-   Organize by feature first
-   Keep bounded contexts isolated
-   Separate domain from infrastructure
-   Minimize package visibility

------------------------------------------------------------------------

# Standard Layout

``` text
com.company.platform.lead
├── api
│   ├── controller
│   ├── request
│   └── response
├── application
│   ├── command
│   ├── query
│   ├── service
│   └── mapper
├── domain
│   ├── aggregate
│   ├── entity
│   ├── valueobject
│   ├── event
│   ├── repository
│   ├── service
│   ├── specification
│   └── exception
├── infrastructure
│   ├── persistence
│   ├── messaging
│   ├── cache
│   ├── external
│   └── configuration
└── config
```

------------------------------------------------------------------------

# Package Responsibilities

## api

-   REST Controllers
-   DTOs
-   Validation
-   OpenAPI

## application

-   Use cases
-   Transaction boundaries
-   Command/Query handlers
-   Event publishing

## domain

-   Business rules
-   Aggregates
-   Value Objects
-   Domain Events

## infrastructure

-   JPA
-   Kafka
-   Redis
-   External adapters

------------------------------------------------------------------------

# Naming Rules

-   Packages: lowercase
-   Controllers: \*Controller
-   Services: *ApplicationService or *DomainService
-   Repositories: \*Repository
-   Entities: singular nouns
-   Events: past tense

------------------------------------------------------------------------

# Dependency Rules

``` text
api
 ↓
application
 ↓
domain
 ↑
infrastructure
```

No package may bypass the application layer.

------------------------------------------------------------------------

# Anti-Patterns

Avoid

-   util packages with business logic
-   shared entities across bounded contexts
-   controllers calling repositories
-   infrastructure depending on api

------------------------------------------------------------------------

# Review Checklist

-   Correct package
-   Correct responsibility
-   Dependency rules respected
-   Feature-first organization
-   No cyclic package references

------------------------------------------------------------------------

# Related Knowledge

-   01-module-structure.md
-   02-layered-architecture.md
-   03-ddd-standards.md

# End
