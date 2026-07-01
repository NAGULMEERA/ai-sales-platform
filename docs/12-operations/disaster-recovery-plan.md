# Disaster Recovery Plan

## RTO & RPO Targets

| Component | RTO | RPO |
|-----------|-----|-----|
| PostgreSQL | < 4 hours | < 15 minutes |
| Kafka | < 2 hours | < 15 minutes |
| Redis | < 1 hour | < 30 minutes |
| Application (K8s) | < 30 minutes | N/A (stateless) |
| **Overall platform** | **< 4 hours** | **< 15 minutes** |

---

## Backup Strategy

### PostgreSQL

- Full backup: daily at 02:00 UTC
- WAL archiving: every 15 minutes to S3
- Retention: 30 days daily, 12 months monthly

### Kafka

- Replication factor ≥ 3 in production
- MirrorMaker 2 for cross-region (future)

### Redis

- AOF + RDB snapshots to S3

---

## Recovery Procedure

1. **Infrastructure** — Restore K8s cluster (Terraform / managed service)
2. **Database** — Restore latest backup + replay WAL to RPO point
3. **Redis** — Restore from AOF/RDB
4. **Kafka** — Verify partition leadership and consumer lag
5. **Validation** — Run smoke tests (health, auth, create lead)
6. **Traffic** — Update DNS / ingress to restored environment

---

## DR Drills

- **Frequency:** Quarterly
- **Automation:** Scripted DR environment provisioning
- **Deliverable:** Post-drill report with RTO/RPO actuals and improvements

---

## Related

- [backup-and-restore.md](backup-and-restore.md)
- [incident-response-runbook.md](incident-response-runbook.md)
