# API Surface (implemented)

External clients should call the **API Gateway** (`:8080`). Paths below are as routed in `GatewayConfig` and implemented by controllers. OpenAPI YAML exists today for identity, tenant, and lead only; other services are documented here from code.

## Auth & tenancy

| Method | Path | Service |
|--------|------|---------|
| POST | `/api/v1/auth/login`, `/register`, `/refresh`, password/verify flows | identity-service |
| GET | JWKS (identity JWKS controller) | identity-service |
| CRUD | `/api/v1/users/**` | identity-service |
| CRUD | `/api/v1/tenants/**`, `/api/v1/tenant-users/**` | tenant-service |

## Business

| Prefix | Service | Controllers |
|--------|---------|-------------|
| `/api/v1/leads/**` | lead-service | `LeadController`, `PipelineController` |
| `/api/v1/customers/**` | customer-service | `CustomerController` |
| `/api/v1/catalog/**` | catalog-service | product/offer/match/recommend |
| `/api/v1/conversations/**` | conversation-service | `ConversationController` |
| `/api/v1/opportunities/**`, `/api/v1/quotes/**` | deal-service | `OpportunityController`, `QuoteController` |

## AI

| Method | Path | Controller |
|--------|------|------------|
| POST | `/api/v1/ai/execute`, `/api/v1/ai/qualify` | `AiGatewayController` |
| CRUD/lifecycle | `/api/v1/prompts/**` | `PromptController` |
| POST | `/api/v1/embeddings` | `EmbeddingController` |
| GET/PUT | `/api/v1/ai-quota/**` | `AiQuotaController` |
| GET | `/api/v1/token-usage/summary` | `TokenUsageController` |
| CRUD/index/ingest | `/api/v1/knowledge-bases/**`, `/api/v1/knowledge-documents/**` | `KnowledgeController` |

## Platform capabilities

| Prefix | Service | Notes |
|--------|---------|-------|
| `/api/v1/workflows/**` | workflow-service | rules + executions (`WorkflowRuleController`) |
| `/api/v1/search/**` | search-service | POST search, global, entity scopes, semantic, autocomplete |
| `/api/v1/analytics/**` | analytics-service | dashboard, funnel, sources, trends, pipeline, AI usage |
| `/api/v1/media/**` | media-service | upload/download URL + metadata |
| `/api/v1/notifications/**` | notification-service | send APIs |
| `/api/v1/invoices/**`, `/api/v1/payments/**` | billing-service | includes Stripe webhooks under payments |
| `/api/v1/marketplace/**` | marketplace-service | plugin registry |
| `/api/v1/integrations/**` | integration-service | Meta / Twilio webhooks |

## Rate-limited routes (gateway)

Configured in `GatewayConfig` for: auth login/register/refresh, password flows, billing webhooks, integration webhooks, AI execute, lead writes, media upload, search, analytics.

## Contracts

Request/response DTOs live in `backend/common/common-contracts`. Feign clients under `com.aisales.common.contracts.client`.

## OpenAPI files present

| Spec | Path |
|------|------|
| Identity | [identity-service-openapi.yaml](identity-service-openapi.yaml) |
| Tenant | [tenant-service-openapi.yaml](tenant-service-openapi.yaml) |
| Lead | [lead-service-openapi.yaml](lead-service-openapi.yaml) |

Other services: use this surface map + controller annotations until OpenAPI is generated for them.

## Standards

- [error-handling-standards.md](error-handling-standards.md)
- [api-versioning-strategy.md](api-versioning-strategy.md)
- [../17-reference/api-reference.md](../17-reference/api-reference.md)
