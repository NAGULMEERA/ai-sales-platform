# Integration test harness

Reusable utilities for platform integration tests (PostgreSQL, Kafka, Flyway).

## Containers

```java
@Container
static PostgreSQLContainer<?> postgres = PlatformTestcontainers.postgres("lead_db");

@Container
static KafkaContainer kafka = PlatformTestcontainers.kafka();
```

Use `@Testcontainers(disabledWithoutDocker = true)` so CI without Docker skips gracefully.

## Flyway

```java
FlywayTestSupport.migrate(dataSource, "classpath:db/migration");
```

## End-to-end event pipeline

See `common-events/src/test/java/.../integration/OutboxToInboxIntegrationIT.java` for the reference test:

publish (outbox) → dispatch → Kafka → inbox consumer → handler.

## Service adoption

1. Add `common-testing` test scope dependency.
2. Copy `LeadFlywayMigrationIT` pattern for migration validation.
3. For event flows, extend patterns from `OutboxToInboxIntegrationIT`.
