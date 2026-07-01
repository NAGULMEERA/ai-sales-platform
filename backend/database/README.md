# Database Migrations – AI Sales Platform

## Overview

| Path | Purpose | Production? |
|------|---------|-------------|
| `migrations-monolith/` | Full-platform lab schema (single PostgreSQL DB) | **No – lab only** |
| `service-splits/` | Per-microservice DDL derived from monolith | **Yes – target topology** |
| `db-html-v7-lineage.md` | Table coverage map from DB.html v7.0 | Reference |

## Lab Database (Monolith)

**LAB ONLY – not production topology.** Validates DDL, ERD, and queries against PostgreSQL 17 + pgvector.

### Prerequisites

- PostgreSQL 17+
- Extensions: `pgcrypto`, `pg_trgm`, `vector`, `btree_gin`, `btree_gist`
- Flyway CLI

### Extract & Fix (from repo root)

```powershell
powershell -File scripts/extract-dds-migrations.ps1
powershell -File scripts/fix-dds-monolith-migrations.ps1
```

### Run Flyway

```bash
flyway -configFiles=database/flyway-monolith.conf migrate
flyway -configFiles=database/flyway-monolith.conf info
```

### Current monolith inventory

| Status | Versions |
|--------|----------|
| Extracted from DDS.html | V001–V014, V028 |
| Architect fixes applied | V002/V003/V004/V005 FK order, V004 `assigned_to`, V007 index, V008 duplicate `version`, V028 dedupe |
| Generated from DB.html v7 | V015–V027, V029 seed, V030–V032 (human agent, ML registry, agent gap) |
| **Total runnable** | **32 files** (V001–V032, includes V028 indexes) |

See `db-html-v7-lineage.md` for section-to-migration mapping.

### Regenerate from updated DB.html

```bash
python scripts/generate-migrations-from-db-html.py
python scripts/split-conversation-service-migrations.py
```

### Lab validation (requires Docker Desktop)

```powershell
# Start Postgres + pgvector and run all monolith migrations
powershell -File scripts/run-lab-flyway.ps1
```

Or manually:

```powershell
cd database
docker compose -f docker-compose-lab.yml up -d postgres-lab
docker compose -f docker-compose-lab.yml --profile migrate run --rm flyway-monolith `
  -url=jdbc:postgresql://postgres-lab:5432/ai_sales_platform `
  -user=aisales -password=aisales -schemas=public `
  -locations=filesystem:/flyway/sql -baselineOnMigrate=true migrate
```

Lab Postgres listens on **port 5433** (avoids conflict with dev compose on 5432).

### Lead service validation (Testcontainers)

Run from the `backend/` directory:

```powershell
cd backend
.\mvnw.cmd -pl services/lead-service -am test "-Dtest=LeadFlywayMigrationIT" "-Dsurefire.failIfNoSpecifiedTests=false"
```

### Known lab limitations

- Cross-service FKs remain (intentional for single-DB lab)
- V006 includes real-estate tables (to split to plugin in production)
- DDS V010 and DB.html §25 overlap (`tool_registry` in DDS; `agent_plans`, `agent_memory`, `tool_calls` in V032 with `IF NOT EXISTS`)
- Some tables in DB.html duplicate DDS names with different shapes (lab uses DDS DDL first)

## Production (Database-per-Service)

Each microservice owns:

```
backend/services/{service}/src/main/resources/db/migration/
```

Cross-service relationships use **UUID references only** (no FK). See `service-split-map.md`.

## Scripts

| Script | Purpose |
|--------|---------|
| `scripts/extract-dds-migrations.ps1` | Parse DDS.html → `migrations-monolith/` |
| `scripts/fix-dds-monolith-migrations.ps1` | Apply FK order and index fixes |
| `scripts/generate-migrations-from-db-html.py` | Generate V015–V032, views, seed from DB.html v7 |
| `scripts/run-lab-flyway.ps1` | Docker lab Postgres + Flyway migrate |
| `scripts/split-lead-service-migrations.ps1` | Build production lead-service DDL |
| `scripts/split-conversation-service-migrations.py` | Build production conversation-service DDL from V015 |
