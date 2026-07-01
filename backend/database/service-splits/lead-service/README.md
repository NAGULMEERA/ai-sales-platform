# Lead Service – Production Migrations (Reference)

Derived from `database/migrations-monolith/V004__lead.sql` with **cross-service FKs removed**.

| File | Purpose |
|------|---------|
| V001__foundation.sql | Extensions, `lead_status` ENUM, triggers |
| V002__lead_tables.sql | All lead aggregate tables |
| V003__lead_indexes.sql | GIN, covering, FTS indexes |

## Deploy to service

When approved, copy into:

```
backend/services/lead-service/src/main/resources/db/migration/
```

Remove or replace existing `V1__init_lead_db.sql`.

## UUID references (no FK)

- `tenant_id` – resolved via tenant context / events
- `customer_id` – customer-service owns record
- `assigned_to`, `created_by`, `updated_by` – identity-service owns users

Regenerate after monolith changes:

```powershell
powershell -File scripts/split-lead-service-migrations.ps1
```
