# Tenant Onboarding Strategy

## Onboarding Flow

```
Signup / Sales-assisted provisioning
    ↓
Create Tenant (tenant-service)
    ↓
Assign Plan / Start Trial (billing-service)
    ↓
Provision default roles + permissions
    ↓
Create Admin User (identity-service)
    ↓
Enable default features (tenant_features)
    ↓
Send welcome notification
    ↓
Onboarding workflow complete event
```

---

## Orchestration

Long-running onboarding uses **Workflow Service** (saga pattern):

| Step | Service | Compensation |
|------|---------|--------------|
| Create tenant | tenant-service | Delete tenant |
| Create subscription | billing-service | Cancel subscription |
| Create admin user | identity-service | Deactivate user |
| Seed features | tenant-service | Disable features |

---

## Customer Health Score

Monitor tenant activation (see monolith function pattern in gap analysis):

| Signal | Weight |
|--------|--------|
| Leads created (7 days) | +20 |
| AI usage (7 days) | +15 |
| Appointments booked (7 days) | +25 |
| Multiple active users | +20 |
| WhatsApp conversations (7 days) | +20 |

Implement in `analytics-service` as scheduled job.

---

## APIs

| Endpoint | Owner | Access |
|----------|-------|--------|
| `POST /api/v1/tenants` | tenant-service | Public / sales |
| `POST /api/v1/auth/register` | identity-service | Public (future) |
| `POST /api/v1/billing/subscriptions` | billing-service | Authenticated admin |

---

## Related

- [billing-architecture.md](../04-domain/billing-architecture.md)
- Events: `TenantCreatedEvent`, `UserCreatedEvent`
