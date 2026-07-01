# Error Code Catalog

Central registry of platform error codes. Enum source: `backend/common/common-exception/.../ErrorCode.java`.

## Authentication & Authorization

| Code | HTTP | Default Message |
|------|------|-----------------|
| AUTH_001 | 401 | Invalid or expired token |
| AUTH_002 | 403 | Insufficient permissions |
| ERR_004 | 401 | Unauthorized |
| ERR_005 | 403 | Access forbidden |

## Tenant

| Code | HTTP | Default Message |
|------|------|-----------------|
| TENANT_001 | 403 | Tenant is inactive or suspended |
| TENANT_002 | 403 | Cross-tenant access denied |
| ERR_007 | 400 | Tenant operation failed |

## Lead

| Code | HTTP | Default Message |
|------|------|-----------------|
| LEAD_001 | 400 | Invalid lead data |
| LEAD_002 | 404 | Lead not found |

## AI

| Code | HTTP | Default Message |
|------|------|-----------------|
| AI_001 | 429 | AI rate limit exceeded |
| AI_002 | 503 | AI service unavailable |

## Billing

| Code | HTTP | Default Message |
|------|------|-----------------|
| BILLING_001 | 402 | Payment required to continue |

## Platform Generic

| Code | HTTP | Default Message |
|------|------|-----------------|
| ERR_001 | 500 | Internal server error |
| ERR_002 | 400 | Validation failed |
| ERR_003 | 404 | Resource not found |
| ERR_006 | 422 | Business rule violation |
| ERR_008 | 500 | Failed to publish event |
| ERR_009 | 409 | Resource conflict |
| ERR_010 | 503 | Service unavailable |

## Adding New Codes

1. Add enum value to `ErrorCode.java` using `{DOMAIN}_{NNN}` pattern.
2. Register in this catalog.
3. Throw via `BusinessException` or domain-specific exception subclasses.
