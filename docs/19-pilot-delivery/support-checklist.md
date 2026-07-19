# Support Checklist

Prepare Customer Success / L1–L2 before pilot traffic.

## People

- [ ] Named Delivery Manager + Technical Account contact for each pilot tenant
- [ ] On-call rotation published (primary / secondary)
- [ ] Escalation path to engineering for Sev-1/2 ([incident-response-guide.md](./incident-response-guide.md))

## Channels

- [ ] Shared Slack/Teams channel per pilot (or ticket queue)
- [ ] Status communication template (degraded / restored)
- [ ] After-hours contact agreed in MSA/SOW

## Knowledge

- [ ] CS trained on [admin-manual.md](./admin-manual.md) and [api-usage-guide.md](./api-usage-guide.md)
- [ ] Known Limitations shared with customer in writing
- [ ] Industry plugin enablement steps practiced (Natural Farming / RE / Auto)
- [ ] Error code cheat sheet: [error-code-catalog.md](../17-reference/error-code-catalog.md)

## Tooling

- [ ] Access to Grafana/Prometheus (read-only) for CS lead
- [ ] Ability to look up `correlation_id` / `trace_id` from customer reports
- [ ] Tenant ID / organization ID recorded in CRM for each pilot

## SLAs (pilot defaults — adjust per contract)

| Severity | First response | Update cadence |
|----------|----------------|----------------|
| Sev-1 (outage) | 15–30 min | Every 30 min |
| Sev-2 (major degrade) | 1 hour | Every 2 hours |
| Sev-3 (partial) | 1 business day | Daily |
| Sev-4 (how-to) | 2 business days | As needed |

## Exit criteria

- [ ] First successful onboarding completed with CS shadow
- [ ] One simulated incident drill completed with CS + on-call
