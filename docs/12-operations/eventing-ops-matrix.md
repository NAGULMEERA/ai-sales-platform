# Eventing Operations Matrix

Platform eventing uses **Outbox** (publish) and **Inbox** (consume) in `common-events`, with Kafka DLT topics (`*.DLT`) and `dead_letter` persistence where enabled.

## Per-service posture

| Service | Outbox | Inbox | Typical topics / notes |
|---------|--------|-------|------------------------|
| identity-service | yes | yes | Auth/tenant lifecycle events |
| tenant-service | yes | yes | Tenant lifecycle |
| lead-service | yes | yes | Lead domain/integration events |
| customer-service | yes | yes | Customer events |
| conversation-service | yes | yes | Message / AI insight events |
| catalog-service | yes | yes | Catalog match events |
| deal-service | yes | yes | Opportunity events |
| workflow-service | yes | yes | Workflow triggered/completed |
| ai-service | yes | varies | Prompt/knowledge events |
| search-service | yes | yes | Indexer consumers |
| analytics-service | yes | yes | Fact recording consumers |
| notification-service | no* | yes | Consumes email/notification intents (*outbox disabled by design) |
| media / appointment / billing / audit / integration | check service YAML | check service YAML | Enable only if the service publishes/consumes |

\* Notification is primarily a consumer/delivery worker; it should not invent business events.

## Ops checks

1. Metric: pending outbox count / publish failures.
2. Kafka consumer lag per group.
3. DLT depth and `dead_letter` table growth.
4. After deploy: confirm no sustained outbox growth and inbox claim leases renew.

## Failure handling

| Failure | Automatic behavior | Operator action |
|---------|--------------------|-----------------|
| Transient Kafka error | Retry + backoff (Resilience4j) | Wait; check Kafka |
| Poison message | DLT + dead_letter row | Inspect payload; fix consumer; replay |
| Duplicate delivery | Inbox idempotency | No action if inbox healthy |
| Publisher crash mid-flight | Outbox claim lease expiry + reclaim | Ensure multiple publisher instances |

## Related

- ADR / platform event docs under `docs/`
- [alerting-and-slos.md](alerting-and-slos.md)
