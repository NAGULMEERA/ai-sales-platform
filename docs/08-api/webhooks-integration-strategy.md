# Webhooks & Integration Strategy

## Outbound Webhooks

```
Domain Event (Kafka)
    → Integration Service dispatcher
    → Filter by tenant webhook config
    → HMAC-SHA256 signature
    → HTTP POST with retry + exponential backoff
    → Dead letter after max retries
```

Schema: `V016__integration.sql` (`webhooks`, `webhook_logs`)

---

## Security

- HMAC-SHA256 signature header: `X-Webhook-Signature`
- Timestamp + replay protection
- Idempotency key per delivery

---

## Inbound Webhooks

| Provider | Owner |
|----------|-------|
| Razorpay / Stripe | billing-service → integration ACL |
| WhatsApp (Meta) | integration-service / capability plugin |
| CRM connectors | integration-service |

---

## Pre-built Integrations (Roadmap)

| Integration | Priority |
|-------------|----------|
| Zapier | High |
| Slack | High |
| HubSpot | Medium |
| Salesforce | Medium |

---

## Related

- Service: `backend/services/integration-service/`
- DDL: `V016__integration.sql`
