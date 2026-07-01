# 08-security-standards.md

# Security Engineering Standards

Version: 1.0

## Purpose

Defines mandatory security engineering standards for all services.

------------------------------------------------------------------------

# Security Principles

-   Zero Trust
-   Least Privilege
-   Defense in Depth
-   Secure by Default
-   Privacy by Design

------------------------------------------------------------------------

# Authentication

Use

-   JWT
-   OAuth2/OIDC
-   Service Accounts

Never trust client input without validation.

------------------------------------------------------------------------

# Authorization

Implement

-   RBAC
-   Tenant validation
-   Resource ownership checks

Authorize every protected request.

------------------------------------------------------------------------

# Secrets

Store secrets in external secret management.

Never commit

-   Passwords
-   API Keys
-   Tokens
-   Certificates

------------------------------------------------------------------------

# Input Validation

Validate

-   Request bodies
-   Query parameters
-   Path variables
-   Uploaded files

Reject invalid input early.

------------------------------------------------------------------------

# Data Protection

Encrypt

-   Data in transit (TLS)
-   Sensitive data at rest

Hash passwords with adaptive algorithms.

------------------------------------------------------------------------

# Secure Coding

Always

-   Parameterize SQL
-   Escape output
-   Validate file uploads
-   Limit request size

Never

-   Log secrets
-   Expose stack traces
-   Concatenate SQL

------------------------------------------------------------------------

# API Security

Implement

-   HTTPS
-   Rate limiting
-   Idempotency
-   Security headers
-   CORS policy

------------------------------------------------------------------------

# AI Security

Protect against

-   Prompt injection
-   Data leakage
-   Unsafe tool execution

Validate every AI response.

------------------------------------------------------------------------

# Dependency Security

-   Scan dependencies
-   Patch vulnerabilities
-   Remove unused libraries
-   Track CVEs

------------------------------------------------------------------------

# Security Testing

Perform

-   SAST
-   Dependency scanning
-   Secret scanning
-   Penetration testing

------------------------------------------------------------------------

# Review Checklist

-   Authentication verified
-   Authorization verified
-   Secrets externalized
-   Validation implemented
-   Encryption enabled
-   Security tests passing

------------------------------------------------------------------------

# Related Knowledge

-   08-security-architecture.md
-   02-spring-boot-standards.md
-   04-rest-api-standards.md

# End
