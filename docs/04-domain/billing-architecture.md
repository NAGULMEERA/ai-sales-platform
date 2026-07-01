# Billing & Subscription Architecture

## Ownership

**Billing Service** owns plans, subscriptions, invoices, payments, usage metering.

Schema reference: `backend/database/migrations-monolith/V017__billing.sql`

---

## Payment Gateways

| Region | Provider | ACL Location |
|--------|----------|--------------|
| India | Razorpay | `integration-service` capability plugin |
| International | Stripe | `integration-service` capability plugin |

Never embed payment logic in business services.

---

## Subscription States

```
TRIAL → ACTIVE → PAST_DUE → SUSPENDED → CANCELLED → EXPIRED
```

| State | Platform Access |
|-------|-----------------|
| TRIAL | Full (within trial limits) |
| ACTIVE | Full |
| PAST_DUE | Grace period (configurable) |
| SUSPENDED | Read-only |
| CANCELLED | Until period end |
| EXPIRED | Blocked (`BILLING_001`) |

---

## Proration

| Action | Behavior |
|--------|----------|
| Upgrade mid-cycle | Charge prorated difference immediately |
| Downgrade mid-cycle | Credit applied to next invoice |
| Cancel mid-cycle | Access until period end, no refund (default) |

---

## Event Flow

```
PaymentReceived (webhook)
    → Billing Service validates + idempotent processing
    → SubscriptionUpdated event
    → Tenant Service updates limits/features
    → Notification Service sends receipt
```

---

## Webhook Idempotency

Store `provider_event_id` with unique constraint; duplicate webhooks return 200 without side effects.

---

## Related

- Monolith DDL: `V017__billing.sql`
- Service: `backend/services/billing-service/` (implementation pending)
- Events: `PaymentCompletedEvent` in `common-events`
