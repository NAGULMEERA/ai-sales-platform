# ADR-002: JWT Authentication

**Status**: Accepted

## Context

Services need stateless authentication with tenant-aware authorization.

## Decision

Use JWT access + refresh tokens issued by identity-service. Tokens include tenantId, userId, and roles claims.

## Consequences

- Shared secret management via Config Server
- Token revocation handled via refresh token store
