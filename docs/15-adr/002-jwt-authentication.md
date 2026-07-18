# ADR-002: JWT Authentication

**Status**: Accepted (superseded in part by RSA/JWKS rollout)

## Context

Services need stateless authentication with tenant-aware authorization.

## Decision

Use JWT access + opaque refresh tokens issued by identity-service.

- **Access tokens:** RS256 (RSA). Identity signs with the private key; other services validate via JWKS (`/.well-known/jwks.json`) or the configured public key.
- **Refresh tokens:** Opaque, stored in PostgreSQL, rotated on use.
- Claims include `tenantId`, `userId`, roles, and permissions.

## Consequences

- Private key stays in identity-service / Vault (`JWT_PRIVATE_KEY_PEM`); never distributed to other services.
- Shared HMAC secret (`aisales.security.jwt.secret`) is retired.
- Token revocation for long-lived sessions is handled via the refresh-token store.
- Production secrets follow ADR-011 (Vault / External Secrets Operator).
