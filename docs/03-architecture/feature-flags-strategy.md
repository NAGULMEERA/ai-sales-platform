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

Current evaluation API: `FeatureCheckService` in **identity-service** (subscription features).

```java
featureCheckService.isFeatureEnabled(tenantId, "voice_ai");
```

A shared `FeatureFlagEvaluator` contract was removed as unused dead code; reintroduce in `common-contracts` only when multiple services need a Feign/SDK-facing evaluator.

Cache in Redis with TTL; invalidate on admin update.

---

## Related

- DDL: `tenant_features` in `V003__tenant.sql`
- Code: `backend/services/identity-service/.../FeatureCheckService.java`
- Debt notes: `docs/13-quality/tech-debt-review.md`
