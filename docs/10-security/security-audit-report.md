# Security Audit Report

Principal Security Engineer review of the AI Sales Platform.  
Companion canvas: IDE canvas `principal-security-audit`.

## Scope

All backend services, `common-security`, API gateway, AI/RAG paths, media uploads, webhooks, secrets/deploy patterns. No architecture redesign.

## Verdict

**Design-partner / limited pilot: YES** (with mitigations applied in this pass).  
**Enterprise SSO / compliance GA: NO** until audit-service, IdP federation, and upload AV are complete.

**Overall posture score: 72 / 100**

## Inventory reused

| Capability | Location |
|------------|----------|
| JWT filter / RS256 / JWKS | `common-security`, identity `JwksController`, gateway filter |
| Tenant authorization | `TenantAuthorizationAspect`, `@PreAuthorizeTenant` |
| Gateway Redis rate limits | `infrastructure/api-gateway` RateLimiterConfig |
| Prompt sanitizer | `PromptVariableSanitizer` |
| RAG tenant SQL | `KnowledgeChunkVectorRepository`, `KeywordRetriever` |
| Webhook HMAC | Stripe + Meta verifiers |
| Secrets pattern | Vault → ESO → K8s Secret (`deployment/kubernetes/external-secrets`) |
| Email redaction | notification (now delegates to `SensitiveDataRedactor`) |

## Controls applied this pass

1. **Media upload hardening** — `MediaContentValidator` (allowlist, blocked extensions, magic bytes).
2. **Prompt injection reject** — `rejectIfInjection` before sanitize on AI execute.
3. **HTTP security headers** — `SecurityHeadersCustomizer` on platform + identity/tenant/notification; gateway headers.
4. **CORS allowlist** — disabled by default; enable via `CORS_ALLOWED_ORIGINS` / `aisales.security.cors.allowed-origins`.
5. **Shared PII redactor** — `common-core` `SensitiveDataRedactor`.
6. **ErrorCode.AI_PROMPT_INJECTION** — reserved for structured rejection telemetry.

## Residual open items

| Priority | Item |
|----------|------|
| P0 | Complete `audit-service` consumer / immutable audit store |
| P0 | Enterprise OIDC / IdP federation |
| P1 | Antivirus scanning for uploads |
| P1 | Broader RBAC permission consistency + hierarchy |
| P1 | Cross-service tenant isolation integration tests |
| P2 | Field-level PII encryption at rest |

## OWASP Top 10 (summary)

See canvas for full mapping. Aggregate **B+**: access control and injection paths are solid after this pass; logging/monitoring and crypto at-rest remain the weaker categories.

## Related

- [multi-tenant-isolation-strategy.md](multi-tenant-isolation-strategy.md)
- [audit-trail-strategy.md](audit-trail-strategy.md)
- [oauth2-oidc-strategy.md](oauth2-oidc-strategy.md)
- [../15-adr/adr-011-secrets-management.md](../15-adr/adr-011-secrets-management.md)
