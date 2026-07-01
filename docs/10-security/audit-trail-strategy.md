# Audit Trail Strategy

## Ownership

**Audit Service** owns immutable audit logs. Schema: `V019__audit.sql`.

---

## Audit Data Model

Every audit record captures:

| Dimension | Fields |
|-----------|--------|
| Who | `user_id`, `user_email`, `user_ip`, `user_agent` |
| What | `action`, `resource_type`, `resource_id` |
| When | `created_at` (timestamptz) |
| Change | `old_values`, `new_values`, `diff` (JSONB) |
| Trace | `correlation_id`, `tenant_id` |

---

## Capture Mechanism

1. `@Auditable` annotation on application service methods
2. AOP aspect publishes `AuditEvent` to Kafka
3. Audit Service persists append-only log
4. Hash chain (`audit_hash_chain`) ensures tamper detection

---

## Compliance

- **Retention:** 7+ years (configurable per tenant tier)
- **Immutability:** No UPDATE/DELETE on audit_log
- **Export:** Compliance export API (CSV/JSON) for enterprise

---

## Related

- Code: `@Auditable` in `common-core`, implementation pending in audit-service
- DDL: `V019__audit.sql`
