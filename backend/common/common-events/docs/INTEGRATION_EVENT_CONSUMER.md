# Integration event consumer — platform pattern

Use `IntegrationEventListener` from `common-events` for idempotent Kafka consumption.

## 1. Flyway

Copy `common-events/src/main/resources/db/platform/V1__platform_eventing_inbox.sql` into your service migrations.

## 2. Handler

```java
@Service
@RequiredArgsConstructor
public class TenantCreatedConsumer {

    private final IntegrationEventListener integrationEventListener;

    @KafkaListener(
            topics = "${aisales.events.default-topic:aisales-events}",
            groupId = "${spring.application.name}",
            containerFactory = "integrationKafkaListenerContainerFactory")
    public void onTenantCreated(ConsumerRecord<String, String> record) {
        integrationEventListener.handle(
                record,
                "lead-service",
                TenantCreatedEvent.class,
                this::processTenantCreated);
    }

    private void processTenantCreated(TenantCreatedEvent event) {
        // business logic
    }
}
```

## 3. Properties

```yaml
spring:
  application:
    name: lead-service
  kafka:
    bootstrap-servers: localhost:9092

aisales:
  events:
    inbox:
      enabled: true
    default-topic: aisales-events
```

## Pipeline

```
Kafka record
  ↓ headers (X-Correlation-Id, X-Tenant-Id)
  ↓ parse JSON → BaseEvent
  ↓ processed_events lookup (skip if duplicate)
  ↓ handler
  ↓ mark processed
  ↓ on failure → dead_letter
```
