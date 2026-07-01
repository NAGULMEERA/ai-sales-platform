# Error Handling & Observability Standards

## Standard Error Response

All public APIs return this format via `GlobalExceptionHandler`:

```json
{
  "timestamp": "2026-06-28T10:30:00Z",
  "correlationId": "abc-123-def-456",
  "traceId": "7f8a9b0c1d2e3f4a",
  "status": 400,
  "error": {
    "code": "LEAD_001",
    "message": "Phone number is required",
    "details": {
      "field": "phone",
      "rejectedValue": null,
      "validationRule": "not null"
    }
  },
  "path": "/api/v1/leads",
  "tenantId": "550e8400-e29b-41d4-a716-446655440000",
  "userId": "550e8400-e29b-41d4-a716-446655440100",
  "validationErrors": []
}
```

Implementation: `backend/common/common-exception/`

---

## Error Code Catalog

See [error-code-catalog.md](../17-reference/error-code-catalog.md) for the full registry.

| Code | HTTP | Message | User Action |
|------|------|---------|-------------|
| AUTH_001 | 401 | Invalid token | Re-authenticate |
| AUTH_002 | 403 | Insufficient permissions | Contact admin |
| TENANT_001 | 403 | Tenant inactive | Contact support |
| TENANT_002 | 403 | Cross-tenant access denied | — |
| LEAD_001 | 400 | Invalid lead data | Check input |
| LEAD_002 | 404 | Lead not found | Verify ID |
| AI_001 | 429 | Rate limit exceeded | Wait and retry |
| AI_002 | 503 | AI service unavailable | Retry later |
| BILLING_001 | 402 | Payment required | Update payment |

---

## Observability Stack

| Pillar | Technology | Location |
|--------|------------|----------|
| Metrics | Micrometer + Prometheus + Grafana | `infrastructure/monitoring/` |
| Logs | Structured JSON (SLF4J + MDC) | `common-observability/LoggingFilter` |
| Traces | OpenTelemetry + Zipkin | `docker-compose-infra.yml` |
| Health | Spring Actuator | `/actuator/health`, `/actuator/info` |

### Required MDC fields

`correlationId`, `traceId`, `tenantId`, `userId`, `service_name`

---

## Rules

- Never expose stack traces, SQL errors, or internal paths.
- Every error includes `correlationId`; include `traceId` when tracing is active.
- Log errors at WARN for business exceptions, ERROR for unexpected failures.
- Never log passwords, tokens, or secrets.

---

## Related

- Rule 08 — Observability Governance
- Code: `backend/common/common-exception/`, `backend/common/common-observability/`
