# SSO & Social Login Strategy

## Supported Providers

| Provider | Protocol | Use Case |
|----------|----------|----------|
| Google | OIDC | Consumer / self-service |
| Microsoft Entra ID | OIDC / SAML | Enterprise B2B |
| LinkedIn | OIDC | B2B sales |
| Custom SAML | SAML 2.0 | Enterprise SSO |

---

## User Mapping

External identity → internal `User`:

```
provider + provider_subject → users.external_provider_id
email → users.email (match or create)
default role → SALES_EXECUTIVE (configurable per tenant)
tenant_id → from signup context or domain mapping
```

Implementation target: `identity-service` OAuth2 client + Keycloak identity brokering.

---

## Related

- [oauth2-oidc-strategy.md](oauth2-oidc-strategy.md)
