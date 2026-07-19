# Pilot Delivery Pack

**Audience:** SaaS Delivery, Customer Success, On-Call, Security  
**Scope:** Limited design-partner / pilot customers (not enterprise GA)  
**Ground truth:** Implementation + existing ops/security docs — no invented features

## Verdict

| Gate | Status |
|------|--------|
| Limited pilot | **GO** with checklist sign-off |
| Enterprise SSO / compliance GA | **NO-GO** until audit-service, IdP federation, upload AV (see Known Limitations) |

## Documents in this pack

| Document | Purpose |
|----------|---------|
| [pilot-deployment-checklist.md](./pilot-deployment-checklist.md) | Deploy + smoke for first pilot tenants |
| [production-checklist.md](./production-checklist.md) | Prod hardening gate (extends DevOps) |
| [monitoring-checklist.md](./monitoring-checklist.md) | Observability before go-live |
| [backup-checklist.md](./backup-checklist.md) | Backup/restore verification |
| [security-checklist.md](./security-checklist.md) | Pilot security gate |
| [support-checklist.md](./support-checklist.md) | Support readiness |
| [incident-response-guide.md](./incident-response-guide.md) | Pilot-facing pointer + severity |
| [customer-onboarding-guide.md](./customer-onboarding-guide.md) | Tenant enablement runbook |
| [api-usage-guide.md](./api-usage-guide.md) | Pilot integrator API path |
| [admin-manual.md](./admin-manual.md) | Tenant admin day-2 ops |
| [release-notes.md](./release-notes.md) | Pilot release notes |
| [known-limitations.md](./known-limitations.md) | Honest out-of-scope / residual risk |
| [operational-runbook.md](./operational-runbook.md) | Ops fast path for pilots |
| [go-live-checklist.md](./go-live-checklist.md) | Final go/no-go sign-off |

## Related existing assets (do not duplicate)

- Deploy: [../11-devops/production-deployment.md](../11-devops/production-deployment.md), [../11-devops/deployment-guide.md](../11-devops/deployment-guide.md)
- Ops: [../12-operations/runbook.md](../12-operations/runbook.md), [../12-operations/alerting-and-slos.md](../12-operations/alerting-and-slos.md)
- Security: [../10-security/security-audit-report.md](../10-security/security-audit-report.md)
- API: [../08-api/api-surface.md](../08-api/api-surface.md)
- Verticals: [../16-roadmap/natural-farming-vertical.md](../16-roadmap/natural-farming-vertical.md)

## Recommended pilot industries

1. Natural Farming (`POST /api/v1/plugins/natural-farming/enable`) — first production vertical  
2. Real Estate or Automobile — already vertically validated
