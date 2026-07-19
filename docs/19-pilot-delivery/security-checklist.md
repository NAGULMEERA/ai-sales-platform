# Security Checklist (Pilot)

Source of truth: [security-audit-report.md](../10-security/security-audit-report.md) (posture ~72; pilot YES / GA NO).

## Must-pass for pilot

- [ ] JWT RS256; access tokens only on business APIs
- [ ] Issuer/audience enforcement enabled in pilot/prod
- [ ] Tenant isolation: cross-tenant read fails in smoke test
- [ ] Gateway rate limits on auth, AI, media, webhooks
- [ ] Security headers present on gateway/API responses
- [ ] CORS allowlist (or CORS disabled)
- [ ] Prompt injection rejection enabled on AI execute
- [ ] Media upload allowlist + magic-byte checks
- [ ] Webhook HMAC verified (Stripe/Meta) when channels live
- [ ] Secrets only via env/secret manager — no git secrets in runtime
- [ ] Admin accounts MFA policy agreed (process) even if IdP MFA not yet wired

## Disclose to customer (do not block pilot)

- [ ] No enterprise IdP federation yet (JWT platform-issued)
- [ ] `audit-service` immutable store incomplete
- [ ] No antivirus scanning on uploads yet
- [ ] Field-level PII encryption at rest not complete

## Access control

- [ ] Pilot tenant admin users listed and least-privilege roles assigned
- [ ] Service accounts / Feign credentials rotated
- [ ] Break-glass procedure documented

## Sign-off

| Role | Sign | Date |
|------|------|------|
| Security Engineer | | |
| Delivery Manager | | |

**If any Must-pass item fails → NO-GO.**
