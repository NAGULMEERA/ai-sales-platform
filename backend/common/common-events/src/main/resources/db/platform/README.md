# Platform eventing migrations

Per-service Flyway migrations. Each microservice owns its own copy in `src/main/resources/db/migration/`.

| File | Purpose |
|------|---------|
| `V1__platform_eventing_inbox.sql` | `processed_events` + `dead_letter` |
| Outbox | Service-specific (see tenant-service `V6__create_outbox_events.sql`) |

Do not share tables across services. Copy SQL, do not reference this path from Flyway at runtime.
