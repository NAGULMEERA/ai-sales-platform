# OAuth2 / OIDC Strategy

## Decision

**Phase 1 (current):** Custom JWT issued by Identity Service (RS256 + JWKS) for development velocity.

**Phase 2 (production):** **Keycloak** as primary IdP with Spring Authorization Server fallback for internal services.

---

## Supported Flows

| Flow | Use Case |
|------|----------|
| Authorization Code + PKCE | Web and mobile apps |
| Client Credentials | Service-to-service |
| Refresh Token Rotation | All user sessions |
| Device Flow | Headless / IoT (future) |

---

## Architecture

```
┌─────────────────────────────────────────┐
│  Keycloak / OAuth2 Provider             │
│  - User management                      │
│  - JWT issuance (RS256 / JWKS)          │
│  - Social login (Google, Microsoft)     │
│  - Enterprise SSO (SAML, OIDC)          │
└──────────────────┬──────────────────────┘
                   │
                   ▼
┌─────────────────────────────────────────┐
│  API Gateway + Spring Security          │
│  - Token validation (JWKS)              │
│  - RBAC enforcement                     │
│  - Tenant resolution from claims        │
└──────────────────┬──────────────────────┘
                   │
                   ▼
┌─────────────────────────────────────────┐
│  Identity Service                       │
│  - User federation mapping              │
│  - Internal user ↔ external subject     │
└─────────────────────────────────────────┘
```

---

## JWT Claims (required)

```json
{
  "sub": "user-uuid",
  "tenant_id": "tenant-uuid",
  "organization_id": "org-uuid",
  "roles": ["SALES_MANAGER"],
  "permissions": ["lead:read", "lead:create"],
  "session_id": "session-uuid"
}
```

---

## External Identity Providers

| Provider | Protocol | Use Case |
|----------|----------|----------|
| Google | OIDC | B2C / self-service signup |
| Microsoft Entra ID | OIDC / SAML | Enterprise B2B |
| LinkedIn | OIDC | B2B sales teams |

User federation maps external `sub` + provider → internal `User` entity in Identity Service.

---

## Migration Path

1. Deploy Keycloak with platform realm and clients.
2. Configure `spring.security.oauth2.resourceserver.jwt.issuer-uri` in services.
3. Gateway validates JWT via JWKS (replace header-only check).
4. Identity Service syncs users from Keycloak events.
5. After Keycloak cutover, deprecate custom Identity-issued tokens (already RS256) after a migration window.

---

## Related

- [SSO and Social Login](sso-and-social-login.md)
- ADR: [002-jwt-authentication](../15-adr/002-jwt-authentication.md)
- Code: `backend/common/common-security/`, `backend/services/identity-service/`
