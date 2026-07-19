# Domain Model

Aggregates and entities as implemented under `backend/services/*/domain/entity`. Each service owns its database; cross-service references use IDs + events/Feign, not shared tables.

## Bounded contexts → services

| Bounded context | Owning service | Primary aggregates / entities |
|-----------------|----------------|-------------------------------|
| Identity & access | identity-service | `User`, `RefreshToken`, `UserSession`, `Permission`, `RolePermission`, `Tenant` (identity copy), `TenantSubscription`, `SubscriptionFeature`, tokens, `AuditLogEntry` |
| Tenant admin | tenant-service | `Tenant`, `User` (tenant-users), `Role`, `TenantAuditLog` |
| Lead | lead-service | `Lead` + notes, tags, activities, followups, assignments, attachments, attributions, scores, duplicates, pipeline (`SalesPipeline*`) |
| Customer | customer-service | `Customer` + addresses, contacts, consent, interactions, timeline, duplicates |
| Catalog | catalog-service | `CatalogProduct`, `CatalogOffer` |
| Conversation | conversation-service | `ConversationThread`, `ConversationMessage`, participants, attachments, timeline |
| Deal | deal-service | `Opportunity`, `Quote`, `QuoteLineItem`, opportunity timeline |
| AI | ai-service | `PromptTemplate`, `PromptVersionEntity`, `KnowledgeBase`, `KnowledgeDocument`, `KnowledgeChunk`, `TokenUsage`, quotas |
| Workflow | workflow-service | `WorkflowExecution`, `WorkflowRule`, `WorkflowAutomationExecution`, `WorkflowExecutionHistory` |
| Search | search-service | `SearchDocument` (projection) |
| Analytics | analytics-service | `AnalyticsEvent`, `AnalyticsDailyRollup` |
| Billing | billing-service | `Invoice`, `InvoiceLineItem`, `Payment`, `StripeWebhookEvent` |
| Media | media-service | `MediaObject` (metadata only; bytes in S3) |
| Notification | notification-service | Channel/templates/delivery (see service package; not all modeled as JPA entities listed above) |
| Marketplace | marketplace-service | `PluginCatalogEntry`, `PluginInstallation` |
| Integration | integration-service | `MetaPageBinding`, `VoiceCall`, `IntegrationWebhookEvent` |

## Aggregate ownership rules (implemented)

- **Lead** is system of record for lead lifecycle; conversion may call customer/deal via Feign and publish events.
- **Customer** owns contact methods and merge; lead conversion creates/links customers without sharing tables.
- **Opportunity / Quote** live in deal-service; lead conversion creates opportunities through deal APIs.
- **AI artifacts** (prompts, embeddings, chunks) never hold lead/customer business state.
- **Workflow** stores execution/rule state only; actions call owning services or publish events (`WorkflowActionExecutor`).
- **Search / Analytics** are read models derived from events.

## Key domain enums (contracts)

Located under `backend/common/common-contracts`:

- Lead / customer / catalog / deal status and type enums used by DTOs and entities
- `WorkflowTriggerType`, `WorkflowActionType`, `WorkflowConditionType`
- `ContactMethodType`, `SearchEntityType`

## Scaffolds

`appointment-service` and `audit-service` do not yet expose domain aggregates (health only).

## Related

- [../15-adr/adr-026-lead-aggregate.md](../15-adr/adr-026-lead-aggregate.md)
- [../15-adr/adr-027-opportunity-aggregate.md](../15-adr/adr-027-opportunity-aggregate.md)
- [../15-adr/adr-029-conversation-model.md](../15-adr/adr-029-conversation-model.md)
- [../07-microservices/service-catalog.md](../07-microservices/service-catalog.md)
