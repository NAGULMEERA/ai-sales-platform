# API Versioning Strategy

## Style

**URL path versioning:** `/api/v1/leads`, `/api/v2/leads`

Constants: `ApiConstants.API_V1`, `ApiConstants.API_V2` in `common-core`.

---

## Version Policy

| Change Type | Version Impact |
|-------------|----------------|
| Breaking change (remove field, change semantics) | New major version (`v2`) |
| Add optional field / endpoint | Same version |
| Bug fix | Same version (patch release) |

---

## Deprecation Policy

- **Minimum 2 major versions** supported concurrently.
- **6 months notice** before sunsetting (`ApiConstants.DEPRECATION_NOTICE_MONTHS`).
- Response headers on deprecated endpoints:
  - `Deprecation: true`
  - `Sunset: Sat, 31 Dec 2026 23:59:59 GMT`
  - `Link: </api/v2/leads>; rel="successor-version"`

---

## OpenAPI

Mark deprecated endpoints:

```yaml
/api/v1/leads:
  get:
    deprecated: true
    x-deprecation-date: 2026-12-31
    x-sunset-date: 2027-06-30
```

---

## Gateway Routing

Add parallel routes in `GatewayConfig` when v2 launches:

```java
.route("lead-service-v2", r -> r.path("/api/v2/leads/**").uri("lb://lead-service"))
```

---

## Contract Testing

- Consumer-driven contract tests per version.
- Regression suite runs against v1 and v2 until v1 sunset.

---

## Related

- Rule 04 — REST API Governance
- Code: `backend/common/common-core/.../ApiConstants.java`
