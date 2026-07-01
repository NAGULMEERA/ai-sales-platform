# 04-rest-api-standards.md

# REST API Engineering Standards

Version: 1.0

## Purpose

Defines REST API standards for the AI Sales Employee Platform to ensure
consistency, security, maintainability, and backward compatibility.

------------------------------------------------------------------------

# API Design Principles

-   Resource-oriented URLs
-   Stateless requests
-   Consistent HTTP semantics
-   Versioned APIs
-   DTOs only across API boundaries

------------------------------------------------------------------------

# URL Conventions

Use plural nouns.

Examples

``` text
GET    /api/v1/leads
GET    /api/v1/leads/{id}
POST   /api/v1/leads
PUT    /api/v1/leads/{id}
PATCH  /api/v1/leads/{id}
DELETE /api/v1/leads/{id}
```

Never expose repository or database concepts.

------------------------------------------------------------------------

# HTTP Methods

-   GET: Read
-   POST: Create
-   PUT: Replace
-   PATCH: Partial update
-   DELETE: Remove/Archive

GET must never modify state.

------------------------------------------------------------------------

# Request & Response DTOs

-   Separate request and response models
-   Never expose JPA entities
-   Validate requests with Bean Validation
-   Return immutable response DTOs

------------------------------------------------------------------------

# Validation

Use

-   @Valid
-   @NotNull
-   @NotBlank
-   @Size
-   Custom business validators

Return validation failures using RFC 7807 Problem Details.

------------------------------------------------------------------------

# Pagination

Use

-   page
-   size
-   sort

Example

``` text
GET /api/v1/leads?page=0&size=20&sort=createdAt,desc
```

------------------------------------------------------------------------

# Filtering

Support explicit filters.

Example

``` text
GET /api/v1/leads?status=QUALIFIED&source=WHATSAPP
```

------------------------------------------------------------------------

# Error Handling

Use standard HTTP status codes.

-   200 OK
-   201 Created
-   204 No Content
-   400 Bad Request
-   401 Unauthorized
-   403 Forbidden
-   404 Not Found
-   409 Conflict
-   422 Unprocessable Entity
-   500 Internal Server Error

Return Problem Details with correlation ID.

------------------------------------------------------------------------

# Idempotency

Required for

-   Payment APIs
-   Webhooks
-   External callbacks

Support Idempotency-Key header.

------------------------------------------------------------------------

# Versioning

Expose versions in URL.

``` text
/api/v1
```

Never introduce breaking changes without a new version.

------------------------------------------------------------------------

# Security

-   JWT/OAuth2
-   Tenant validation
-   RBAC
-   Rate limiting
-   Input sanitization

------------------------------------------------------------------------

# Documentation

Every endpoint must include

-   OpenAPI definition
-   Request examples
-   Response examples
-   Error responses
-   Security requirements

------------------------------------------------------------------------

# API Review Checklist

-   Resource-oriented URL
-   Correct HTTP verb
-   DTOs only
-   Validation implemented
-   Pagination supported
-   Problem Details used
-   OpenAPI updated
-   Security verified

------------------------------------------------------------------------

# Related Knowledge

-   02-spring-boot-standards.md
-   08-security-architecture.md
-   03-ddd-standards.md

# End
