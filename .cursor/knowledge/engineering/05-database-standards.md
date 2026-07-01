# 05-database-standards.md

# Database Engineering Standards

Version: 1.0

## Purpose

Defines PostgreSQL, Flyway, and persistence standards for the AI Sales
Employee Platform to ensure scalability, consistency, maintainability,
and performance.

------------------------------------------------------------------------

# Database Principles

-   PostgreSQL is the primary relational database.
-   One repository per aggregate.
-   Database supports the domain; it does not define it.
-   Migrations are version-controlled.

------------------------------------------------------------------------

# Naming Conventions

Tables

-   snake_case
-   Singular names preferred

Examples

-   lead
-   customer
-   appointment
-   subscription

Columns

-   snake_case
-   Descriptive names

Primary Keys

-   id (UUID preferred)

Foreign Keys

-   `<entity>_id`

Indexes

-   idx\_
    ```{=html}
    <table>
    ```
    \_`<column>`{=html}
-   uq\_
    ```{=html}
    <table>
    ```
    \_`<column>`{=html}

------------------------------------------------------------------------

# Schema Design

Rules

-   Normalize to 3NF unless justified.
-   Avoid duplicate data.
-   Use proper foreign keys.
-   Use check constraints where applicable.
-   Use NOT NULL whenever possible.

------------------------------------------------------------------------

# Multi-Tenancy

Every business table contains

-   tenant_id

Never query tenant data without tenant filtering.

Support optimistic locking with

-   version

------------------------------------------------------------------------

# Auditing

Every aggregate table contains

-   created_at
-   created_by
-   updated_at
-   updated_by
-   version

Critical business actions generate audit records.

------------------------------------------------------------------------

# Soft Deletes

Prefer soft delete using

-   deleted
-   deleted_at

Never physically delete critical business records.

------------------------------------------------------------------------

# Flyway Standards

Rules

-   One migration per change
-   Never edit executed migrations
-   Use semantic descriptions

Example

``` text
V001__create_lead_table.sql
V002__create_customer_table.sql
V003__add_lead_score.sql
```

------------------------------------------------------------------------

# Indexing Strategy

Create indexes for

-   Foreign keys
-   Search columns
-   Tenant filtering
-   Frequently sorted columns

Review unused indexes periodically.

------------------------------------------------------------------------

# Performance

Use

-   Pagination
-   Batch updates
-   Fetch joins carefully
-   Proper indexing
-   Query optimization

Avoid

-   N+1 queries
-   SELECT \*
-   Long-running transactions

------------------------------------------------------------------------

# Transactions

-   Keep transactions short.
-   Use optimistic locking.
-   Avoid distributed transactions when possible.

------------------------------------------------------------------------

# Repository Standards

Repositories

-   Persist aggregates only
-   Return domain objects
-   Never contain business rules
-   Hide SQL details

------------------------------------------------------------------------

# Backup & Recovery

Implement

-   Automated backups
-   Restore validation
-   Point-in-time recovery
-   Disaster recovery testing

------------------------------------------------------------------------

# Database Review Checklist

-   Migration added
-   Naming standards followed
-   Tenant isolation verified
-   Indexes reviewed
-   Constraints defined
-   Audit fields included
-   Repository updated
-   Performance reviewed

------------------------------------------------------------------------

# Related Knowledge

-   02-spring-boot-standards.md
-   03-ddd-standards.md
-   10-deployment-architecture.md

# End
