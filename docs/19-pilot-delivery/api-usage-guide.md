# API Usage Guide (Pilot Integrators)

All external calls go through the **API Gateway**. Surface map: [api-surface.md](../08-api/api-surface.md). Errors: [error-handling-standards.md](../08-api/error-handling-standards.md).

## Base URL

```text
https://{pilot-gateway-host}
```

## Authentication

1. `POST /api/v1/auth/login` → access + refresh tokens  
2. Send `Authorization: Bearer {accessToken}` on protected routes  
3. Refresh via `POST /api/v1/auth/refresh` before expiry  
4. Do **not** send access tokens that are refresh-type (gateway/services reject)

Tenant context comes from JWT claims — **do not** put `tenantId` in request bodies for authorization.

## Correlation

Send or accept `X-Correlation-Id` (see platform headers). Include it in support tickets.

## Idempotency

For retry-safe creates (quotes/leads where supported), send:

```http
Idempotency-Key: {stable-uuid-or-key}
```

## Typical pilot flows

### 1. Create lead (industry attributes)

```http
POST /api/v1/leads
Authorization: Bearer …
Content-Type: application/json

{
  "…": "…",
  "attributes": {
    "cropInterest": "tomato",
    "volumeKg": 500,
    "organicRequired": true
  }
}
```

Attribute keys come from the enabled industry plugin (Natural Farming / RE / Auto).

### 2. AI qualification

```http
POST /api/v1/leads/{id}/ai-qualification
```

or gateway execute:

```http
POST /api/v1/ai/execute
```

AI **recommends**; business services **decide**. Prompt injection attempts return structured errors.

### 3. Catalog match / recommend

```http
POST /api/v1/matches
POST /api/v1/recommendations
```

Use `attributeFilters` aligned to plugin `matchAttributeKeys`.

### 4. Quote

```http
POST /api/v1/quotes
Idempotency-Key: …
```

Line items require catalog `offerId` (platform pricing snapshot).

### 5. Enable industry plugin (platform admin)

```http
POST /api/v1/plugins/natural-farming/enable
```

## OpenAPI (available today)

| Spec | Path under docs/08-api |
|------|------------------------|
| Identity | `identity-service-openapi.yaml` |
| Tenant | `tenant-service-openapi.yaml` |
| Lead | `lead-service-openapi.yaml` |

Other services: use [api-surface.md](../08-api/api-surface.md) + controller annotations until specs are generated.

## Rate limits

Gateway Redis limiters apply to auth, AI execute, media upload, webhooks, search, analytics. Expect `429` with retry guidance.

## Webhooks (if enabled)

- Stripe: signature verified  
- Meta Lead Ads: signature verified  
Never expose unsigned webhook URLs.
