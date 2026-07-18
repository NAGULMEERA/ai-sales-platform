# conversation-service

Owns the **Conversation** bounded context: threads and messages linked to leads/customers.
Industry-agnostic — no WhatsApp/AI provider logic in Phase 2 core.

## Local run

```powershell
docker compose -f deployment/docker-compose-infra.yml up -d postgres kafka
# CREATE DATABASE conversation_db;
cd backend
.\mvnw.cmd spring-boot:run -pl services/conversation-service "-Dspring-boot.run.profiles=local"
```

- Port: **8086**
- Swagger: http://localhost:8086/swagger-ui.html
- DB: `conversation_db` on Postgres **5433**

## Model

| Table | Purpose |
|-------|---------|
| `conversation_thread` | Conversation aggregate (channel, status, leadId/customerId) |
| `conversation_message` | Messages (CUSTOMER / AGENT / SYSTEM / AI) |

Legacy BIGSERIAL `conversations` rows from early scaffold are unused by the new API.

## APIs (`/api/v1/conversations`)

| Method | Path | Description |
|--------|------|-------------|
| POST | `/` | Start (requires `leadId` or `customerId`) |
| GET | `/` | List (`leadId` optional) |
| GET | `/{id}` | Get |
| POST | `/{id}/messages` | Add message |
| GET | `/{id}/messages` | List messages |
| POST | `/{id}/close` | Close |

## Events (outbox → Kafka)

`ConversationStarted`, `ConversationMessageAdded`, `ConversationClosed`

Lead-service appends these to the lead timeline. Workflow-service runs `CONVERSATION_FOLLOWUP_V1`.
