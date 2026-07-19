# Go-Live Checklist

Final gate before pilot customer traffic. All linked checklists must be signed.

## A. Documentation & disclosure

- [ ] [Known Limitations](./known-limitations.md) acknowledged by customer (email/SOW attachment)
- [ ] [Release Notes](./release-notes.md) shared
- [ ] Support channel + SLA confirmed ([support-checklist.md](./support-checklist.md))

## B. Technical readiness

- [ ] [Pilot Deployment Checklist](./pilot-deployment-checklist.md) complete
- [ ] [Production Checklist](./production-checklist.md) complete
- [ ] [Monitoring Checklist](./monitoring-checklist.md) complete
- [ ] [Backup Checklist](./backup-checklist.md) complete
- [ ] [Security Checklist](./security-checklist.md) Must-pass items complete

## C. Customer readiness

- [ ] [Customer Onboarding Guide](./customer-onboarding-guide.md) Day 0–1 done
- [ ] Tenant admin completed [Admin Manual](./admin-manual.md) walkthrough
- [ ] Integrator has [API Usage Guide](./api-usage-guide.md)
- [ ] Industry plugin enabled and catalog seeded
- [ ] First lead + AI + quote (if in scope) demonstrated live

## D. People readiness

- [ ] On-call staffed for go-live window (+24h)
- [ ] CS coverage for first business week
- [ ] Rollback owner named; Helm revision / previous image tags known

## E. Go / No-Go

| Question | Yes/No |
|----------|--------|
| Any Sev-1 risk open? | |
| Security Must-pass failed? | |
| Backup restore unverified? | |
| Customer expects GA-only features (SSO/AV/audit warehouse)? | |

**GO** only if all answers are **No** (no blocking risks) and sections A–D checked.

## Sign-off

| Role | Name | Date | Decision (GO / NO-GO) |
|------|------|------|------------------------|
| Delivery Manager | | | |
| Engineering Lead | | | |
| Security | | | |
| Customer Success | | | |
| Customer sponsor (optional) | | | |

## Post go-live (T+24h)

- [ ] No Sev-1; Sev-2s tracked  
- [ ] Error budgets reviewed  
- [ ] Customer pulse call scheduled  
- [ ] Lessons added to operational runbook
