# Incident Response Guide (Pilot)

This guide is the **pilot-facing entry point**. Detailed engineering steps live in the ops runbook.

## Primary runbook

→ [incident-response-runbook.md](../12-operations/incident-response-runbook.md)

Also: [runbook.md](../12-operations/runbook.md) · [operational-guide.md](../12-operations/operational-guide.md) · [alerting-and-slos.md](../12-operations/alerting-and-slos.md)

## Severity (pilot)

| Sev | Definition | Examples |
|-----|------------|----------|
| 1 | Platform down or data exposure risk | Gateway 5xx storm; auth completely broken; suspected cross-tenant leak |
| 2 | Major feature unusable | AI provider down; Kafka lag blocking lead events; billing webhooks failing |
| 3 | Degraded / workaround exists | Search lag; single service elevated latency |
| 4 | Cosmetic / how-to | UI confusion; doc gaps |

## First 15 minutes

1. Acknowledge alert / customer report; open incident channel  
2. Capture `correlation_id` / `trace_id` / tenant_id / time window  
3. Check gateway, identity, Postgres, Kafka, Redis health  
4. Decide: mitigate (scale/rollback/disable feature) vs investigate  
5. Customer update within SLA ([support-checklist.md](./support-checklist.md))

## Common pilot mitigations

| Symptom | Action |
|---------|--------|
| Auth failures | Check JWT keys / clock skew / iss-aud; rollback identity if needed |
| AI timeouts | Fail closed with quota/provider message; switch provider if configured |
| Event lag | Scale consumers; inspect DLT; do not delete outbox rows |
| Rate limit storms | Confirm not attack; temporarily raise limiter only with security approval |
| Suspected tenant leak | Freeze affected tenants; preserve logs; escalate Security Sev-1 |

## Post-incident

- [ ] Timeline + root cause written  
- [ ] Customer notified of resolution  
- [ ] Follow-ups filed (code, alert, doc)  
- [ ] Known Limitations updated if residual risk remains
