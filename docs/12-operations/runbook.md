# Runbook Index

Primary incident and operational runbooks for on-call engineers.

| Situation | Document |
|-----------|----------|
| Sev classification, triage, escalation | [incident-response-runbook.md](incident-response-runbook.md) |
| Daily ops, health, quota, event lag | [operational-guide.md](operational-guide.md) |
| Postgres backup/restore | [backup-and-restore.md](backup-and-restore.md) |
| Full site restore | [disaster-recovery-guide.md](disaster-recovery-guide.md) |
| Alert → action mapping | [alerting-and-slos.md](alerting-and-slos.md) |
| Outbox / inbox / DLT ownership | [eventing-ops-matrix.md](eventing-ops-matrix.md) |
| K8s production checklist | [../11-devops/kubernetes-production-checklist.md](../11-devops/kubernetes-production-checklist.md) |
| Rolling deploy + Flyway | [../11-devops/rolling-deploy-and-flyway.md](../11-devops/rolling-deploy-and-flyway.md) |

## Fast path — service down

1. Confirm blast radius (gateway vs single service) via health and Prometheus alerts.
2. Check recent deploy / Flyway migration failures.
3. Check Postgres connectivity and connection pool saturation.
4. Check Kafka consumer lag for that service’s group.
5. Roll back or scale replicas per [incident-response-runbook.md](incident-response-runbook.md).

## Fast path — auth failures spike

1. Verify identity-service and JWKS.
2. Confirm gateway JWT config (issuer/audience) matches minted tokens.
3. Check clock skew and key rotation.
4. Review `JwtAuthenticationFilter` tenant header mismatch (`X-Tenant-Id` vs token).
