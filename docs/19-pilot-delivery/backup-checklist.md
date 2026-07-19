# Backup Checklist

Grounded in [backup-and-restore.md](../12-operations/backup-and-restore.md) and [disaster-recovery-guide.md](../12-operations/disaster-recovery-guide.md).

## Schedule

- [ ] Automated Postgres backups for every service database (or cluster with per-DB dumps)
- [ ] Retention: ≥ 7 daily + ≥ 4 weekly for pilot
- [ ] Backup encryption at rest confirmed
- [ ] Redis: document whether ephemeral (rate limits / cache) — rebuild OK
- [ ] S3/media bucket versioning + lifecycle (if S3 enabled)
- [ ] Kafka: retention adequate for replay needs; not a backup substitute for Postgres

## Verification (before go-live)

- [ ] Restore one non-prod DB from latest backup successfully
- [ ] Flyway schema version matches restored data
- [ ] Identity login works against restored identity DB
- [ ] Document restore RTO/RPO vs [disaster-recovery-plan.md](../12-operations/disaster-recovery-plan.md)

## Pilot day-0

- [ ] Backup job succeeded in last 24h
- [ ] On-call knows restore contact + runbook link
- [ ] Customer data residency / retention expectations recorded

## Out of scope for pilot (disclose)

- Multi-region active-active
- Point-in-time cross-service coordinated restore (use DR ordered restore)
