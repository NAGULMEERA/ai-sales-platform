# ai-service

Owns the **AI** bounded context: prompt registry, AI gateway execution, embeddings,
semantic cache, and knowledge metadata.

Business services **never** call LLM vendors. They call this service (Feign / gateway)
and **validate** structured AI output before changing business state.

## Local run

```powershell
docker compose -f deployment/docker-compose-infra.yml up -d postgres kafka
# CREATE DATABASE ai_db;  (or use deployment/postgres/init-databases.sql)
cd backend
.\mvnw.cmd spring-boot:run -pl services/ai-service "-Dspring-boot.run.profiles=local"
```

- Port: **8088**
- Swagger: http://localhost:8088/swagger-ui.html
- DB: `ai_db` on Postgres **5433**
- LLM provider (local): **STUB** (`aisales.ai.llm.provider=STUB`)

## Model

| Capability | Purpose |
|------------|---------|
| Prompt registry | Versioned templates (`code`, purpose, `{{variables}}`) |
| AI Gateway | `POST /api/v1/ai/execute` — render + provider port + structured output |
| Knowledge metadata | KB + document refs (`mediaId` / `objectKey`); no binary storage |
| Embeddings | Existing `POST /api/v1/embeddings` (BGE-M3 / optional OpenAI) |

## APIs

| Method | Path | Description |
|--------|------|-------------|
| POST | `/api/v1/prompts` | Create prompt + v1 |
| GET | `/api/v1/prompts` | List prompts |
| GET | `/api/v1/prompts/{id}` | Get prompt |
| GET | `/api/v1/prompts/by-code/{code}` | Get by code |
| POST | `/api/v1/prompts/{id}/versions` | New immutable version |
| GET | `/api/v1/prompts/{id}/versions` | List versions |
| POST | `/api/v1/ai/execute` | Execute prompt via gateway |
| POST | `/api/v1/knowledge-bases` | Create KB |
| GET | `/api/v1/knowledge-bases` | List KBs |
| GET | `/api/v1/knowledge-bases/{id}` | Get KB |
| POST | `/api/v1/knowledge-bases/{id}/documents` | Register document metadata |
| GET | `/api/v1/knowledge-bases/{id}/documents` | List documents |
| GET | `/api/v1/knowledge-documents/{id}` | Get document |
| POST | `/api/v1/embeddings` | Embed text (existing) |

Requires JWT with `tenantId`.

## Events (outbox → Kafka)

`PromptExecuted`, `KnowledgeBaseCreated`, `KnowledgeDocumentRegistered`

## Feign client

`com.aisales.common.contracts.client.AiServiceClient`

## Non-goals (this phase)

- Full RAG ingest (chunk/embed/retrieve)
- Voice / WhatsApp AI
- Multi-agent orchestration
- Vendor SDKs in business services
