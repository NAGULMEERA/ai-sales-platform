# Sequence Diagrams

Flows below reflect existing classes and APIs. They do not describe unimplemented product features.

## 1. Authenticated API call (gateway → service)

```mermaid
sequenceDiagram
    autonumber
    actor Client
    participant GW as API Gateway
    participant ID as identity-service
    participant SVC as Business service

    Client->>ID: POST /api/v1/auth/login
    ID-->>Client: access + refresh JWT
    Client->>GW: API + Authorization Bearer
    GW->>GW: JWT validate / rate limit
    GW->>SVC: Proxied /api/v1/**
    SVC->>SVC: JwtAuthenticationFilter + TenantContext
    SVC-->>GW: ApiResponse DTO
    GW-->>Client: HTTP response
```

Sources: `AuthController`, `GatewayConfig`, `JwtAuthenticationFilter`.

## 2. Lead created → workflow + search + analytics

```mermaid
sequenceDiagram
    autonumber
    actor Agent
    participant Lead as lead-service
    participant OB as Outbox
    participant Kafka as Kafka
    participant WF as workflow-service
    participant Search as search-service
    participant Analytics as analytics-service

    Agent->>Lead: POST /api/v1/leads
    Lead->>Lead: Persist Lead + outbox LeadCreated
    Lead-->>Agent: 201 LeadDto
    Lead->>OB: Commit outbox_events
    OB->>Kafka: Publish LeadCreated
    Kafka->>WF: LeadLifecycleEventConsumer / WorkflowAutomationEventConsumer
    WF->>WF: startOnLeadCreated / automation rules
    Kafka->>Search: SearchIndexEventConsumer upsert LEAD
    Kafka->>Analytics: AnalyticsEventConsumer record lead.created
```

Sources: `LeadCreatedEvent.EVENT_TYPE`, consumers in workflow/search/analytics services, outbox tables per service.

## 3. AI execute with optional RAG

```mermaid
sequenceDiagram
    autonumber
    actor Caller
    participant AI as AiGatewayService
    participant Prompt as PromptService
    participant RAG as KnowledgeRetrievalService
    participant Hybrid as HybridRetriever
    participant LLM as LlmProvider
    participant Quota as AiQuotaService

    Caller->>AI: POST /api/v1/ai/execute
    AI->>Prompt: Resolve template + version
    AI->>AI: PromptRenderer + PromptVariableSanitizer
    opt knowledgeBaseId present
        AI->>RAG: retrieve
        RAG->>Hybrid: vector + keyword RRF
        Hybrid-->>RAG: chunks
        RAG->>AI: KnowledgeContextAssembler (untrusted delimiters)
    end
    AI->>Quota: reserveExecute
    AI->>LLM: complete (outside long DB TX)
    AI->>AI: record token usage + events
    AI-->>Caller: AiExecuteResponse
```

Sources: `AiGatewayController`, `AiGatewayService`, `HybridRetriever`, `KnowledgeContextAssembler`.

## 4. Workflow automation action

```mermaid
sequenceDiagram
    autonumber
    participant Kafka as Kafka
    participant Cons as WorkflowAutomationEventConsumer
    participant Eng as WorkflowAutomationEngine
    participant Exec as WorkflowActionExecutor
    participant Lead as LeadServiceClient
    participant AI as AiServiceClient

    Kafka->>Cons: e.g. LeadQualified
    Cons->>Eng: onTrigger(WorkflowTriggerType, context)
    Eng->>Eng: Load WorkflowRule + evaluate conditions
    Eng->>Exec: execute(WorkflowActionDto)
    alt ASSIGN_LEAD / UPDATE_LEAD
        Exec->>Lead: Feign call
    else CALL_AI_GATEWAY
        Exec->>AI: Feign /execute
    else SEND_EMAIL / PUBLISH_EVENT
        Exec->>Exec: EventPublisher (NotificationSent / WorkflowTriggered)
    end
```

Sources: `WorkflowTriggerType`, `WorkflowActionType`, `WorkflowActionExecutor`.

## Related

- [c4-containers.md](c4-containers.md)
- [../06-ai-platform/ai-architecture.md](../06-ai-platform/ai-architecture.md)
- [../07-microservices/workflow.md](../07-microservices/workflow.md)
