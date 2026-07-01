# Flyway Migration Strategy

## Two-Tier Model

| Tier | Location | Purpose |
|------|----------|---------|
| **Lab monolith** | `backend/database/migrations-monolith/` | Validate full ERD, integration testing |
| **Production per-service** | `backend/services/{service}/src/main/resources/db/migration/` | Database-per-service |

---

## Lab Monolith (V001–V034+)

Validates DDL against PostgreSQL 16 + pgvector. **Not production topology.**

```bash
cd backend/database
docker compose -f docker-compose-lab.yml up -d
../scripts/run-lab-flyway.ps1
```

---

## Production Split

Each microservice owns its schema. Cross-service references are UUID only (no FK).

Split map: `backend/database/service-split-map.md`

Completed splits:
- `lead-service` — V001–V003
- `conversation-service` — V001–V002

---

## Naming Convention

```
V{version}__{description}.sql
```

Examples: `V001__foundation.sql`, `V002__lead_tables.sql`

Never modify applied migrations — create a new version.

---

## Seed Data

- Platform seeds: `V029__seed_data.sql` (lab)
- Production seeds: per-service `R__seed_*.sql` (repeatable) or dedicated seed migration

---

## Testing

Every service with Flyway migrations requires `*FlywayMigrationIT` using Testcontainers + pgvector.

---

## Related

- `backend/database/README.md` (operational runbook)
- Rule 05 — Database Governance
