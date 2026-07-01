# Feature Flags Strategy

## Categories

| Category | Scope | Example |
|----------|-------|---------|
| Global | All tenants | New UI layout |
| Tenant | Per tenant | WhatsApp integration |
| User | Per user | Beta dashboard |
| Percentage | Gradual rollout | 25% of tenants |

---

## Storage

`tenant_features` table (V003__tenant.sql):

- `feature_key`, `enabled`, `rollout_percentage`, `config` (JSONB)

---

## Evaluation

Interface: `FeatureFlagEvaluator` in `common-contracts`

Implementation owner: **tenant-service**

```java
featureFlagEvaluator.isEnabledWithRollout(tenantId, "voice_ai");
```

Cache in Redis with TTL; invalidate on admin update.

---

## Related

- DDL: `tenant_features` in `V003__tenant.sql`
- Code: `backend/common/common-contracts/.../FeatureFlagEvaluator.java`
