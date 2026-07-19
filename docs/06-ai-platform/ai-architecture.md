# AI Architecture

Implementation owner: **ai-service**. Principle: AI recommends; business services decide.

## Responsibilities

| Capability | Classes / APIs |
|------------|----------------|
| Prompt registry | `PromptService`, `PromptController` `/api/v1/prompts` |
| Execute | `AiGatewayService`, `POST /api/v1/ai/execute` |
| Qualify orchestration | `AiQualificationOrchestrator`, `POST /api/v1/ai/qualify` |
| Embeddings | `EmbeddingApplicationService`, `POST /api/v1/embeddings` |
| Knowledge / RAG | See [rag-architecture.md](rag-architecture.md) |
| Quota / budget | `AiQuotaService`, `/api/v1/ai-quota` |
| Token usage | `TokenUsageService`, `/api/v1/token-usage/summary` |
| Semantic cache | `SemanticCacheService` |
| Provider routing | `LlmProvider` port; `OpenAiLlmClient`, `GeminiLlmClient`, `StubLlmProvider` |

## Execute path (implemented)

1. Resolve prompt template + active version (`PromptService`)
2. Sanitize variables (`PromptVariableSanitizer`) and render (`PromptRenderer`)
3. Optional RAG retrieve + assemble untrusted context (`KnowledgeRetrievalService`, `KnowledgeContextAssembler`)
4. Semantic cache lookup (tenant + prompt scope + model)
5. Reserve quota (`AiQuotaService.reserveExecute`)
6. Call `LlmProvider.complete` **outside** long DB transactions
7. Persist usage, publish events (`PromptExecuted`, cache hit/miss, `KnowledgeRetrieved` when applicable)
8. Return `AiExecuteResponse` (rendered prompts only for admin/`ai:debug` when configured)

## Security controls present

- JWT permissions on AI controllers (`ai:execute`, etc.)
- Prompt injection soft-filtering on variables/queries
- RAG context wrapped as untrusted document data
- Provider secrets via configuration / env (not hardcoded in business services)

## What AI must not do

- Persist lead/customer/opportunity aggregates
- Bypass business authorization
- Be called with vendor SDKs from other services (use Feign/`AiServiceClient` or REST through gateway)

## Related

- [ADR-024 AI Gateway](../15-adr/adr-024-ai-gateway.md)
- [rag-architecture.md](rag-architecture.md)
- [embedding-strategy.md](embedding-strategy.md)
