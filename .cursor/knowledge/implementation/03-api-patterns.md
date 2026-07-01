# 03-api-patterns.md

# API Implementation Patterns

Version: 1.0

## Purpose

Defines production-ready REST API implementation patterns for Spring
Boot services in the AI Sales Employee Platform.

------------------------------------------------------------------------

# API Layer Responsibilities

The API layer is responsible for:

-   Receiving HTTP requests
-   Validating input
-   Mapping DTOs
-   Delegating to Application Services
-   Returning standardized responses

The API layer must **never** contain business logic.

------------------------------------------------------------------------

# Standard Flow

``` text
HTTP Request
      │
      ▼
Controller
      │
      ▼
Request Validation
      │
      ▼
Application Service
      │
      ▼
Domain
      │
      ▼
Infrastructure
      │
      ▼
Response DTO
```

------------------------------------------------------------------------

# Controller Pattern

Controllers should

-   Be stateless
-   Use constructor injection
-   Return DTOs only
-   Be thin
-   Delegate immediately

Never

-   Access repositories
-   Call EntityManager
-   Implement business rules

------------------------------------------------------------------------

# Request DTO Pattern

Request DTOs

-   Immutable where possible
-   Bean Validation annotations
-   No persistence annotations

Example fields

-   name
-   email
-   phone
-   budget

------------------------------------------------------------------------

# Response DTO Pattern

Response DTOs

-   Immutable
-   Hide internal IDs where appropriate
-   Never expose JPA entities

------------------------------------------------------------------------

# Validation Pattern

Use

-   @Valid
-   @Validated
-   Custom validators

Validation order

1.  Bean Validation
2.  Business Validation
3.  Domain Validation

------------------------------------------------------------------------

# Pagination Pattern

Support

-   page
-   size
-   sort

Return

-   content
-   totalElements
-   totalPages
-   pageNumber

------------------------------------------------------------------------

# Filtering Pattern

Allow filtering through query parameters.

Example

-   status
-   source
-   assignedUser
-   createdAfter

------------------------------------------------------------------------

# Error Pattern

Return RFC7807 Problem Details.

Include

-   title
-   detail
-   status
-   correlationId
-   timestamp

------------------------------------------------------------------------

# Idempotency Pattern

Required for

-   Payment
-   Webhooks
-   External callbacks

Support

Idempotency-Key header.

------------------------------------------------------------------------

# OpenAPI Pattern

Document

-   Request
-   Response
-   Validation
-   Errors
-   Security
-   Examples

------------------------------------------------------------------------

# Review Checklist

-   Thin controller
-   DTO only
-   Validation implemented
-   Problem Details returned
-   Pagination supported
-   OpenAPI updated

------------------------------------------------------------------------

# Related Knowledge

-   04-rest-api-standards.md
-   02-spring-boot-standards.md
-   01-java-standards.md

# End
