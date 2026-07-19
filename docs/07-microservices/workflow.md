# Workflow Documentation

Owner: **workflow-service**. Business rules stay in owning business services; workflow coordinates.

## Definition keys

`com.aisales.workflow.domain.enums.WorkflowDefinitionKey`:

| Key | Role |
|-----|------|
| `ONBOARDING_V1` | Tenant/user onboarding coordination |
| `LEAD_LIFECYCLE_V1` | Lead created → validated → qualified → assigned |
| `CONVERSATION_FOLLOWUP_V1` | Conversation follow-up coordination |
| `AUTOMATION_RULE_V1` | Tenant-scoped trigger/condition/action rules |

## Built-in lifecycle consumer

`LeadLifecycleEventConsumer` reacts to:

| Event | Service method |
|-------|----------------|
| `LeadCreated` | `LeadLifecycleWorkflowService.startOnLeadCreated` |
| `LeadValidated` | `onLeadValidated` |
| `LeadQualified` | `onLeadQualified` |
| `LeadAssigned` | `completeOnLeadAssigned` |

## Automation engine

`WorkflowAutomationEventConsumer` → `WorkflowAutomationEngine.onTrigger(...)`.

### Triggers (`WorkflowTriggerType`)

`LEAD_CREATED`, `LEAD_QUALIFIED`, `OPPORTUNITY_CREATED`, `CONVERSATION_STARTED`, `MESSAGE_RECEIVED`, `MESSAGE_SENT`, `CUSTOMER_CONVERTED`, `AI_QUALIFICATION_COMPLETED`, `CATALOG_RECOMMENDATION_GENERATED`, `CONVERSATION_CLOSED`, `MANUAL`.

### Actions (`WorkflowActionType`)

| Action | Execution |
|--------|-----------|
| `ASSIGN_LEAD`, `UPDATE_LEAD` | Feign `LeadServiceClient` |
| `CREATE_OPPORTUNITY`, `UPDATE_OPPORTUNITY` | Feign `DealServiceClient` |
| `CALL_AI_GATEWAY` | Feign `AiServiceClient.execute` |
| `SEND_NOTIFICATION`, `SEND_EMAIL`, `SEND_WHATSAPP` | Publish `NotificationSentEvent` |
| `PUBLISH_EVENT`, `CREATE_TASK`, `SCHEDULE_FOLLOW_UP`, `ESCALATE` | Publish `WorkflowTriggeredEvent` coordination |

Conditions: `WorkflowConditionEvaluator` + `WorkflowConditionType` / `WorkflowConditionDto`.

## REST API

`WorkflowRuleController` (`/api/v1/workflows`):

- `POST/GET /rules`, `GET /rules/{id}`, `POST /rules/{id}/disable`
- `GET /executions/{id}`

## Persistence

Entities: `WorkflowRule`, `WorkflowExecution`, `WorkflowAutomationExecution`, `WorkflowExecutionHistory` (+ outbox/inbox tables).

## Related

- [ADR-028](../15-adr/adr-028-workflow-engine.md)
- [sequence-diagrams.md](../03-architecture/sequence-diagrams.md)
