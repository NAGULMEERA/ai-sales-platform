# Microservice Catalog

Deployable services under `backend/services/`.

| Service | Purpose (as implemented) | Primary API prefix(es) |
|---------|--------------------------|------------------------|
| identity-service | Login/register/refresh, JWKS, users, registration, subscription feature checks | `/api/v1/auth`, `/api/v1/users` |
| tenant-service | Tenant CRUD, tenant-users | `/api/v1/tenants`, `/api/v1/tenant-users` |
| lead-service | Lead lifecycle, pipeline, extensions | `/api/v1/leads`, pipeline APIs |
| customer-service | Customer CRUD, merge, contacts, consent | `/api/v1/customers` |
| catalog-service | Products, offers, match, recommend | `/api/v1/catalog` |
| conversation-service | Threads, messages, AI insights hooks | `/api/v1/conversations` |
| deal-service | Opportunities, quotes | `/api/v1/opportunities`, `/api/v1/quotes` |
| ai-service | Execute/qualify, prompts, knowledge, embeddings, quota, token usage | `/api/v1/ai`, `/api/v1/prompts`, `/api/v1/knowledge-*`, … |
| workflow-service | Lifecycle workflows + automation rules | `/api/v1/workflows` |
| notification-service | Notification send APIs | `/api/v1/notifications` |
| billing-service | Invoices, payments, Stripe webhooks | `/api/v1/invoices`, `/api/v1/payments`, … |
| media-service | Media metadata + pre-signed URLs | `/api/v1/media` |
| search-service | Hybrid/global/entity search, autocomplete | `/api/v1/search` |
| analytics-service | Dashboard and metric queries | `/api/v1/analytics` |
| marketplace-service | Plugin catalog/install | `/api/v1/marketplace` |
| integration-service | Meta lead ads + Twilio voice webhooks | `/api/v1/integrations` |
| appointment-service | Health scaffold only | `/api/v1/appointments` (routed) |
| audit-service | Health scaffold only | `/api/v1/audit` (routed) |

## Shared libraries

Under `backend/common/`: `common-core`, `common-security`, `common-events`, `common-contracts`, `common-exception`, `common-observability`, `common-cache`, `common-testing`, `common-starter`.

## Infrastructure processes

| Component | Location |
|-----------|----------|
| API Gateway | `infrastructure/api-gateway` |
| Eureka | infra compose / k8s |
| Config Server | infra compose / k8s |

## Related

- [workflow.md](workflow.md)
- [../08-api/api-surface.md](../08-api/api-surface.md)
- [../03-architecture/c4-containers.md](../03-architecture/c4-containers.md)
