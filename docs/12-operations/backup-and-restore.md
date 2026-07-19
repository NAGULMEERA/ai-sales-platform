# Backup and Restore

## Scope

Primary system of record databases are PostgreSQL (one database per microservice). Kafka and Redis are operational stores with separate retention strategies (see [disaster-recovery-plan.md](disaster-recovery-plan.md)).

## Scripts

| Script | Platform | Purpose |
|--------|----------|---------|
| `scripts/backup-postgres.sh` | Linux/macOS | Custom-format `pg_dump` of all known DBs |
| `scripts/backup-postgres.ps1` | Windows | Same |
| `scripts/restore-postgres.sh` | Linux/macOS | Drop/create + `pg_restore` from a backup directory |

### Backup (example)

```bash
export PGHOST=localhost PGPORT=5432 PGUSER=aisales PGPASSWORD='***'
./scripts/backup-postgres.sh ./backups/postgres
```

```powershell
$env:PGPASSWORD = '***'
.\scripts\backup-postgres.ps1 -OutputDir .\backups\postgres
```

Each run creates a timestamped directory containing `*.dump` files and `MANIFEST.txt`.

### Restore (example)

```bash
export PGHOST=localhost PGPORT=5432 PGUSER=aisales PGPASSWORD='***'
./scripts/restore-postgres.sh ./backups/postgres/20260719T020000Z
```

Restore terminates active sessions, recreates each database, and restores dumps. Use only in controlled DR or staging drills.

## Recommended schedule (production)

| Job | Cadence | Retention |
|-----|---------|-----------|
| Full logical dump (all DBs) | Daily 02:00 UTC | 30 days |
| WAL / continuous archive | Continuous | 15-minute RPO target |
| Monthly archive | 1st of month | 12 months |

Store dumps in object storage (S3/GCS) with encryption at rest and cross-region replication for DR.

## Validation

After restore:

1. Confirm Flyway `flyway_schema_history` matches expected versions.
2. Hit `/actuator/health/readiness` on identity, lead, and gateway.
3. Smoke: login → create lead → search → analytics dashboard.
4. Check Kafka consumer lag and outbox pending metrics.

## Related

- [disaster-recovery-plan.md](disaster-recovery-plan.md)
- [../05-database/flyway-migration-strategy.md](../05-database/flyway-migration-strategy.md)
