# Rolling Deploy and Flyway Upgrade Safety

## Principles

1. **Expand → Migrate → Contract** for schema changes.
2. Never edit an applied Flyway version; add a new migration.
3. Keep services backward compatible across one release wave (N and N-1).
4. Prefer short transactions; keep Feign/AI outside DB transactions.

## Rolling update sequence

1. Apply **additive** Flyway migrations (nullable columns, new tables/indexes).
2. Deploy application versions that write both old and new shapes if needed.
3. Backfill data if required.
4. Deploy readers that depend on the new shape.
5. Later release: drop obsolete columns/tables (contract).

## Graceful shutdown

Platform defaults (`platform/application-observability.yml`):

- `server.shutdown=graceful`
- `spring.lifecycle.timeout-per-shutdown-phase=30s`
- K8s `terminationGracePeriodSeconds: 45` + `preStop sleep 5`

This drains in-flight HTTP before SIGKILL. Outbox publishers should finish the current claim batch within the shutdown window.

## Flyway during rollout

| Situation | Guidance |
|-----------|----------|
| Multiple replicas | Safe if migrations are additive and idempotent |
| Long lock | Avoid heavy `UPDATE` on large tables during peak; use phased migrations |
| Failed migration | Fix forward with a new version; do not rewrite history |

See also `docs/05-database/flyway-migration-strategy.md`.

## Eventing during deploy

- Inbox consumers may briefly lag — monitor Kafka lag and outbox pending.
- Do not disable inbox/outbox in prod to “speed up” deploys.
- After deploy, confirm no DLT spike.

## Verification checklist

- [ ] `/actuator/health/readiness` green on new pods
- [ ] Flyway version matches expected
- [ ] Smoke: auth, lead create, search, notification path
- [ ] No sustained increase in 5xx or outbox lag
