# Disaster Recovery Guide

This guide operationalizes [disaster-recovery-plan.md](disaster-recovery-plan.md) using scripts and topology that exist in the repository.

## Objectives (from DR plan)

| Component | RTO | RPO |
|-----------|-----|-----|
| PostgreSQL | &lt; 4 hours | &lt; 15 minutes |
| Kafka | &lt; 2 hours | &lt; 15 minutes |
| Redis | &lt; 1 hour | &lt; 30 minutes |
| Apps (K8s) | &lt; 30 minutes | N/A (stateless) |
| **Platform** | **&lt; 4 hours** | **&lt; 15 minutes** |

## Backup artifacts

Use scripts in `scripts/`:

| Script | Purpose |
|--------|---------|
| `backup-postgres.sh` / `.ps1` | Logical dump of known service databases |
| `restore-postgres.sh` | Recreate DBs + `pg_restore` |

Details: [backup-and-restore.md](backup-and-restore.md).

Store encrypted dumps off-cluster (object storage) with retention aligned to the DR plan (daily 30 days, monthly 12 months).

## Recovery order

1. **Platform** — Restore Kubernetes control plane / node pool / networking (or managed cluster).
2. **Secrets & config** — Re-apply `deployment/kubernetes/secrets.yml`, config maps, JWT/provider secrets.
3. **PostgreSQL** — Restore dumps (+ WAL replay to RPO if continuous archive is configured in the environment).
4. **Redis** — Restore AOF/RDB if used for required cache/session state; otherwise cold start and accept cache miss.
5. **Kafka** — Restore or rebuild cluster; verify topic `aisales-events` (or configured default); expect consumer lag catch-up via inbox idempotency.
6. **Platform infra pods** — Eureka, Config Server, Gateway.
7. **Domain services** — Start with identity → tenant → business services → ai/workflow/search/analytics.
8. **Validate** — Smoke checks below.
9. **Traffic** — Point ingress/DNS to restored environment.

## Smoke validation checklist

- [ ] Gateway `/actuator/health` (or configured health) OK
- [ ] `POST /api/v1/auth/login` succeeds for a known test user
- [ ] `GET` JWKS reachable for resource servers
- [ ] Create lead (`POST /api/v1/leads`) returns 201
- [ ] Outbox publisher moves events (Kafka UI / consumer lag decreasing)
- [ ] Search returns the new lead after indexing lag
- [ ] Analytics dashboard endpoint responds for tenant
- [ ] Media pre-signed URL generation works if S3 credentials restored

## Eventing during recovery

Consumers are idempotent via inbox (`processed_events`). After Kafka rebuild, prefer **not** double-applying business side effects; rely on inbox claims. If replaying from backup offsets, verify dead-letter tables per [eventing-ops-matrix.md](eventing-ops-matrix.md).

## DR drills

- Cadence: quarterly (per DR plan)
- Record actual RTO/RPO, gaps, and corrective actions in the incident system

## Related

- [disaster-recovery-plan.md](disaster-recovery-plan.md)
- [operational-guide.md](operational-guide.md)
- [../11-devops/deployment-guide.md](../11-devops/deployment-guide.md)
