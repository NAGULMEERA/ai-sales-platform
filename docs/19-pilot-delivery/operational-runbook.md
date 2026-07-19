# Operational Runbook (Pilot)

Fast path for pilot operations. Full index: [runbook.md](../12-operations/runbook.md).

## Daily

- [ ] Check Alertmanager / paging — no Sev-1/2 open  
- [ ] Kafka lag dashboards green  
- [ ] Postgres disk / connection pools OK  
- [ ] Backup job succeeded (last 24h)  
- [ ] AI provider error rate within budget  

## Weekly

- [ ] Review DLT message counts; replay or discard with reason  
- [ ] Review AI token cost vs tenant quotas  
- [ ] Patch OS/node images per DevOps cadence  
- [ ] CS: tenant health (leads, AI usage, active users)

## Deploy

1. [production-checklist.md](./production-checklist.md)  
2. Rolling or blue/green per [blue-green-deployment.md](../11-devops/blue-green-deployment.md)  
3. Flyway: expand-only; [rolling-deploy-and-flyway.md](../11-devops/rolling-deploy-and-flyway.md)  
4. Smoke §5 of [pilot-deployment-checklist.md](./pilot-deployment-checklist.md)

## Incident

→ [incident-response-guide.md](./incident-response-guide.md)

## Eventing

Ownership matrix: [eventing-ops-matrix.md](../12-operations/eventing-ops-matrix.md)

## Useful endpoints

| Check | Path |
|-------|------|
| Liveness | `/actuator/health/liveness` |
| Readiness | `/actuator/health/readiness` |
| Metrics | `/actuator/prometheus` |
| Gateway health | gateway actuator (as deployed) |

## Contacts

Fill for each pilot environment:

| Function | Contact |
|----------|---------|
| On-call primary | |
| Delivery Manager | |
| Security | |
| Customer Success | |
