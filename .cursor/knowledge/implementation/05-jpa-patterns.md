# 05-jpa-patterns.md

# JPA Implementation Patterns

Version: 1.0

## Purpose

Defines production-ready JPA and Spring Data implementation patterns for
the AI Sales Employee Platform.

------------------------------------------------------------------------

# Persistence Principles

-   Persist aggregates, not business workflows.
-   One repository per aggregate root.
-   Keep persistence concerns out of the domain.

------------------------------------------------------------------------

# Entity Mapping

Guidelines

-   One `@Entity` per aggregate root
-   Use `UUID` identifiers
-   Prefer `LAZY` fetching
-   Explicit table and column names

Never expose entities through REST APIs.

------------------------------------------------------------------------

# Aggregate Persistence

Only aggregate roots are loaded directly.

Child entities are modified through aggregate methods.

------------------------------------------------------------------------

# Repository Pattern

Repositories should expose only business-oriented methods.

Typical methods

-   save()
-   findById()
-   existsById()
-   findByTenantId()

Avoid generic query dumping.

------------------------------------------------------------------------

# Fetch Strategy

Prefer

-   LAZY relationships
-   Entity Graphs
-   Fetch Join for specific queries

Avoid

-   Global EAGER loading
-   Multiple collection fetch joins

------------------------------------------------------------------------

# Optimistic Locking

Use

-   `@Version`

Detect concurrent updates and fail safely.

------------------------------------------------------------------------

# Auditing

Enable Spring Data Auditing.

Standard fields

-   createdAt
-   createdBy
-   updatedAt
-   updatedBy

------------------------------------------------------------------------

# Soft Delete

Prefer logical deletion.

Fields

-   deleted
-   deletedAt

Exclude deleted records from normal queries.

------------------------------------------------------------------------

# Query Patterns

Use

-   Spring Data JPA
-   Specifications
-   Pageable
-   Projection DTOs

Avoid native SQL unless justified.

------------------------------------------------------------------------

# Performance

Prevent

-   N+1 queries
-   SELECT \*
-   Large object graphs

Use indexes, pagination and batch processing.

------------------------------------------------------------------------

# Transactions

-   Transaction boundary in application service
-   Read-only for queries
-   Short-lived transactions

------------------------------------------------------------------------

# Flyway Integration

-   Every schema change has a migration
-   Never modify executed migrations
-   Keep migrations backward compatible

------------------------------------------------------------------------

# Review Checklist

-   Correct fetch strategy
-   Optimistic locking enabled
-   Auditing enabled
-   Tenant filtering applied
-   Soft delete respected
-   Queries optimized
-   Migration included

------------------------------------------------------------------------

# Related Knowledge

-   05-database-standards.md
-   04-ddd-implementation.md
-   02-spring-boot-standards.md

# End
