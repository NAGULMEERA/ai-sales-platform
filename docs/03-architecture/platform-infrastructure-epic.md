# Platform Infrastructure Epic



Cross-cutting capabilities every business service (Lead, Customer, AI, Notification, Billing) reuses.



**Status:** Phase 4 done  

**Owner:** Platform team  

**Tenant Service:** merge-ready — no new features until epic milestones land



---



## Phases



| Phase | Scope | Status |

|-------|--------|--------|

| **1 — Event reliability** | Outbox dispatch, inbox idempotency, DLQ, Kafka config, header propagation | Done |

| **2 — Observability** | OpenTelemetry, standard metrics catalog, dashboard templates | Done |

| **3 — Multi-tenant persistence** | RLS Flyway starter, Redis cache module, default audit publisher | Done |

| **4 — Developer experience** | Service scaffold, integration test harness, ADRs | Done |



---



## Phase 1 deliverables



### Outbox (publish side)

- `OutboxEventPublisher` — transactional write (`Propagation.MANDATORY`)

- `OutboxDispatchScheduler` — async Kafka dispatch with retries

- Kafka headers: `X-Correlation-Id`, `X-Tenant-Id`, `X-Event-Id`, `X-Event-Type`



**Enable:** `aisales.events.outbox.enabled=true`



### Inbox (consume side)

- `processed_events` — idempotent consumption per `(event_id, consumer_name)`

- `dead_letter` — failed messages after retries

- `IntegrationEventListener` — standard consume → process → mark processed / DLQ



**Enable:** `aisales.events.inbox.enabled=true` (default)



### Shared Flyway template

Copy from `common-events/src/main/resources/db/platform/V1__platform_eventing_inbox.sql` into each service schema.



### Kafka auto-config

- `KafkaEventsAutoConfiguration` — listener container factory, JSON String serde

- Property prefix: `aisales.events.kafka.*`



---



## Service adoption checklist



1. Add Flyway migration from platform template (inbox + DLQ; outbox if publishing)

2. `aisales.events.outbox.enabled=true` when publishing integration events

3. `aisales.events.inbox.enabled=true` when consuming

4. Inject `IntegrationEventListener` in `@KafkaListener` methods

5. Set `spring.application.name` as consumer name for inbox deduplication



---



## Phase 2 deliverables



### OpenTelemetry tracing

- Replaced Brave/Zipkin with `micrometer-tracing-bridge-otel` + OTLP exporter

- Shared config: `common-observability/src/main/resources/platform/application-observability.yml`

- OTel Collector template: `infrastructure/monitoring/otel-collector-config.yaml`



### Platform metrics catalog

- `MetricNames` — standard business KPI names

- `PlatformMetrics` — tenant-tagged counters/timers with common tags (`application`, `service`, `environment`)



### Kafka trace propagation

- W3C trace context on outbox publish and consumer spans in `IntegrationEventListener`



### Dashboards

- `platform-services-dashboard.json` — HTTP, JVM, tenant KPIs



**Enable:** import `classpath:platform/application-observability.yml` in service `application.yml`



---



## Phase 3 deliverables



### Redis cache (`common-cache`)

- `PlatformCacheService` — tenant-keyed read-through cache

- `TenantCacheKeyGenerator` — `{prefix}:tenant:{tenantId}:{namespace}:{key}`

- Shared config: `common-cache/src/main/resources/platform/application-cache.yml`



**Enable:** `aisales.cache.enabled=true` + import `application-cache.yml`



### RLS Flyway starter

- `common-core/src/main/resources/db/platform/V2__platform_tenant_rls_helpers.sql` — `platform_enable_tenant_rls(schema, table)`

- `V1__platform_service_audit_log.sql` — standard local audit table template

- Docs: `common-core/docs/PERSISTENCE.md`



### Default audit publisher

- `LoggingAuditRecorder` — auto-configured when no custom `AuditRecorder` bean (`aisales.audit.logging-enabled`, default true)

- `EventPublishingAuditRecorder` — optional Kafka audit events (`aisales.audit.publish-events=true`)

- `CompositeAuditRecorder` — fan-out for services combining JDBC + logging + events



---

## Phase 4 deliverables

### Integration test harness (`common-testing`)

- `PlatformTestcontainers` — PostgreSQL + Kafka factories
- `FlywayTestSupport` — migration runner for ITs
- Docs: `common-testing/docs/INTEGRATION_TEST_HARNESS.md`

### End-to-end event pipeline test

- `OutboxToInboxIntegrationIT` in `common-events` — outbox → Kafka → inbox → handler
- Requires Docker (`@Testcontainers(disabledWithoutDocker = true)`)

### Platform service scaffold

- `scripts/scaffold-platform-service.ps1` — platform YAML, outbox/inbox/RLS migrations, DDD packages
- `lead-service` upgraded as Lead-ready skeleton (`PLATFORM.md`)

### ADR

- `docs/15-adr/adr-010-platform-infrastructure-epic.md`

---

## Definition of done (Lead-ready platform)



- [x] Phase 1 merged and documented

- [x] tenant-service and identity-service on platform eventing (inbox; tenant outbox)

- [x] Integration test: publish → outbox → Kafka → inbox → handler

- [x] Correlation ID traceable REST → Kafka → consumer logs

- [x] Phase 2 OTel baseline configured

- [x] Phase 3 Redis cache module available

- [x] Service scaffold generates Lead-service skeleton



---



## Related rules



- Rule 03 — Event Governance (outbox, inbox, idempotency)

- Rule 05 — Database Governance (RLS, tenant columns)

- Rule 08 — Observability (correlation, trace propagation)

- Rule 01 — Microservice Policy (database per service)

