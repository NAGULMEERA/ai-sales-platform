# Communication Patterns

## Synchronous (REST via Feign)
- Service-to-service calls through common-contracts Feign clients
- All external traffic routed through API Gateway

## Asynchronous (Kafka)
- Domain events published via common-events
- Transactional outbox pattern for reliable delivery

## Service Discovery
- Eureka service registry for dynamic routing
- Spring Cloud Config for centralized configuration
