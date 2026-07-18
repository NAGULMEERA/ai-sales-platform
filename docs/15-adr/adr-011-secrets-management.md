# ADR-011: Production Secrets Management

## Status

Accepted

## Context

Platform Rule 06 requires secrets (JWT signing keys, DB passwords, OAuth client secrets, SMTP credentials) to live in Secret Manager or Vault — never in Git, Config Server plaintext, or JKS keystores committed to the repo.

Today JWT uses a shared HMAC secret (`aisales.security.jwt.secret`), not asymmetric keys in a keystore. Services previously mixed hardcoded local defaults with a stub Kubernetes `Secret` that Deployments did not consume.

## Decision

Production secret flow:

```text
Vault / AWS Secrets Manager / GCP Secret Manager
        ↓ External Secrets Operator
Kubernetes Secret (aisales-secrets)
        ↓ env / secretKeyRef
Spring Boot (identity, notification, api-gateway, …)
```

Complementary rules:

1. **Config Server** holds non-secret shared config only (timeouts, feature flags, token TTLs).
2. **Applications** read secrets via environment variables (`JWT_SECRET`, `DB_PASSWORD`, `SMTP_*`, `GOOGLE_CLIENT_*`).
3. **Local/dev** uses `application-local.yml` and optional gitignored `application-local-secrets.yml` — not Vault.
4. **No JKS/PKCS12** in-repo for platform JWT; introduce keystores only if we migrate to asymmetric JWT with a documented rotation process.

## Consequences

- All business-service Deployments (and api-gateway) inject `aisales-secrets` for DB password and JWT; notification also gets SMTP; identity gets Google OAuth; ai-service gets `OPENAI_API_KEY`.
- Example ESO + Vault sync: `deployment/kubernetes/external-secrets/`.
- Bootstrap without Vault: apply `deployment/kubernetes/secrets.yml` placeholders, then replace with ESO.
- Config Server / `config-repo` must not contain passwords or JWT secrets.
- ADR-002’s “JWT secret via Config Server” is superseded for production by this ADR.
