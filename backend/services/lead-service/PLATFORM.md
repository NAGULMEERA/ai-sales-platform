# lead-service — platform adoption

Platform capabilities enabled for Lead-ready development.

## Enabled

- Observability (`application-observability.yml`)
- Cache config import (`application-cache.yml`, enable with `aisales.cache.enabled=true`)
- Outbox event publishing
- Inbox idempotent consumption
- PostgreSQL RLS on tenant-owned tables

## Implementation checklist

1. Domain aggregate: `domain/entity/Lead.java` extending `TenantAwareEntity`
2. Application service: `application/service/LeadService.java`
3. REST API: `api/controller/LeadController.java` at `/api/v1/leads`
4. Publish `LeadCreatedEvent` via outbox on create
5. Consume cross-service events with `IntegrationEventListener`

See `docs/03-architecture/platform-infrastructure-epic.md`.
