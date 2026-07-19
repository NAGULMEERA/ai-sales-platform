# Event Catalog

Source of truth: `backend/common/common-events/src/main/java/com/aisales/common/events/model/*Event.java` (`EVENT_TYPE` constants).

Consumer column lists services that reference `XxxEvent.EVENT_TYPE` in `*EventConsumer` classes. Producers publish via owning services’ outbox; this table does not invent publishers.

Default Kafka topic: `${aisales.events.default-topic:aisales-events}`.

Envelope fields (via `BaseEvent`): `eventId`, `eventType`, `eventVersion`, `tenantId`, `aggregateId`, `correlationId`, `occurredAt`, …

| Event type | Java class | Known consumers |
|------------|------------|-----------------|
| `AIQualificationCompleted` | `AiQualificationCompletedEvent` | analytics-service, workflow-service |
| `AIRecommendationGenerated` | `AiRecommendationGeneratedEvent` | analytics-service |
| `AIReplyGenerated` | `AiReplyGeneratedEvent` | — |
| `CatalogMatched` | `CatalogMatchedEvent` | analytics-service |
| `CatalogOfferCreated` | `CatalogOfferCreatedEvent` | — |
| `CatalogProductCreated` | `CatalogProductCreatedEvent` | search-service |
| `CatalogProductUpdated` | `CatalogProductUpdatedEvent` | search-service |
| `CatalogRecommendationGenerated` | `CatalogRecommendationGeneratedEvent` | analytics-service, workflow-service |
| `ConversationClosed` | `ConversationClosedEvent` | lead-service, workflow-service |
| `ConversationMessageAdded` | `ConversationMessageAddedEvent` | lead-service |
| `ConversationStarted` | `ConversationStartedEvent` | analytics-service, lead-service, search-service, workflow-service |
| `ConversationSummarized` | `ConversationSummarizedEvent` | — |
| `CustomerArchived` | `CustomerArchivedEvent` | — |
| `CustomerCreated` | `CustomerCreatedEvent` | analytics-service, search-service |
| `CustomerDeactivated` | `CustomerDeactivatedEvent` | — |
| `CustomerMerged` | `CustomerMergedEvent` | — |
| `CustomerUpdated` | `CustomerUpdatedEvent` | search-service |
| `CustomerVerified` | `CustomerVerifiedEvent` | — |
| `EmailVerificationRequested` | `EmailVerificationRequestedEvent` | notification-service |
| `EmailVerified` | `EmailVerifiedEvent` | workflow-service |
| `KnowledgeBaseCreated` | `KnowledgeBaseCreatedEvent` | — |
| `KnowledgeDocumentRegistered` | `KnowledgeDocumentRegisteredEvent` | search-service |
| `KnowledgeRetrieved` | `KnowledgeRetrievedEvent` | analytics-service |
| `LeadArchived` | `LeadArchivedEvent` | — |
| `LeadAssigned` | `LeadAssignedEvent` | workflow-service |
| `LeadContacted` | `LeadContactedEvent` | — |
| `LeadConverted` | `LeadConvertedEvent` | analytics-service |
| `LeadConvertedToCustomer` | `LeadConvertedToCustomerEvent` | workflow-service |
| `LeadCreated` | `LeadCreatedEvent` | analytics-service, search-service, workflow-service |
| `LeadLost` | `LeadLostEvent` | — |
| `LeadMerged` | `LeadMergedEvent` | — |
| `LeadQualified` | `LeadQualifiedEvent` | analytics-service, search-service, workflow-service |
| `LeadReopened` | `LeadReopenedEvent` | — |
| `LeadScored` | `LeadScoredEvent` | — |
| `LeadStatusChanged` | `LeadStatusChangedEvent` | analytics-service, search-service |
| `LeadUnassigned` | `LeadUnassignedEvent` | — |
| `LeadValidated` | `LeadValidatedEvent` | workflow-service |
| `LeadVisitCancelled` | `LeadVisitCancelledEvent` | — |
| `LeadVisitCompleted` | `LeadVisitCompletedEvent` | — |
| `LeadVisitScheduled` | `LeadVisitScheduledEvent` | — |
| `MessageReceived` | `MessageReceivedEvent` | analytics-service, workflow-service |
| `MessageSent` | `MessageSentEvent` | analytics-service, workflow-service |
| `NotificationSent` | `NotificationSentEvent` | — |
| `OpportunityAssigned` | `OpportunityAssignedEvent` | — |
| `OpportunityCreated` | `OpportunityCreatedEvent` | analytics-service, search-service, workflow-service |
| `OpportunityLost` | `OpportunityLostEvent` | — |
| `OpportunityStatusChanged` | `OpportunityStatusChangedEvent` | analytics-service, search-service |
| `OpportunityWon` | `OpportunityWonEvent` | — |
| `PasswordResetRequested` | `PasswordResetRequestedEvent` | notification-service |
| `PluginDisabled` | `PluginDisabledEvent` | — |
| `PluginEnabled` | `PluginEnabledEvent` | — |
| `PromptExecuted` | `PromptExecutedEvent` | — |
| `QuoteAccepted` | `QuoteAcceptedEvent` | — |
| `QuoteCreated` | `QuoteCreatedEvent` | — |
| `QuoteSent` | `QuoteSentEvent` | — |
| `SemanticCacheHit` | `SemanticCacheHitEvent` | — |
| `SemanticCacheMiss` | `SemanticCacheMissEvent` | — |
| `SubscriptionPlanChanged` | `SubscriptionPlanChangedEvent` | ai-service |
| `TenantActivated` | `TenantActivatedEvent` | — |
| `TenantCreated` | `TenantCreatedEvent` | ai-service |
| `TenantDeleted` | `TenantDeletedEvent` | — |
| `TenantSuspended` | `TenantSuspendedEvent` | — |
| `TenantUpdated` | `TenantUpdatedEvent` | — |
| `UserCreated` | `UserCreatedEvent` | workflow-service |
| `UserDeleted` | `UserDeletedEvent` | — |
| `UserUpdated` | `UserUpdatedEvent` | — |
| `WorkflowCompleted` | `WorkflowCompletedEvent` | analytics-service |
| `WorkflowTriggered` | `WorkflowTriggeredEvent` | analytics-service |

## Additional audit event

| Event type | Java class | Notes |
|------------|------------|-------|
| `AuditRecorded` | `com.aisales.common.events.audit.AuditRecordedEvent` | Published via common audit recorder path |

## Schemas & legacy package

- JSON schemas: `backend/common/common-events/src/main/resources/schemas/events/*.v1.json` (loaded by `EventSchemaRegistry`).
- Legacy types under `com.aisales.common.events.event` (partial older set, including `AppointmentBookedEvent` / `PaymentCompletedEvent`) are **not** the primary catalog above. Prefer `com.aisales.common.events.model.*` for new work.

## Related

- [ADR-033 Outbox/Inbox](../15-adr/adr-033-outbox-inbox-eventing.md)
- [Eventing ops matrix](../12-operations/eventing-ops-matrix.md)
- `backend/common/common-events/docs/INTEGRATION_EVENT_CONSUMER.md`
