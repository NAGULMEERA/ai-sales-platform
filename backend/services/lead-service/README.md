# lead-service

Owns the **Lead** bounded context: capture, validation, qualification, assignment,
scoring, follow-ups, duplicate detection, and lifecycle events.

## Local run

```powershell
docker compose -f deployment/docker-compose-infra.yml up -d postgres kafka
# CREATE DATABASE lead_db;  (or use deployment/postgres/init-databases.sql)
cd backend
.\mvnw.cmd spring-boot:run -pl services/lead-service "-Dspring-boot.run.profiles=local"
```

- Port: **8083**
- Swagger: http://localhost:8083/swagger-ui.html
- DB: `lead_db` on Postgres **5433**

## Sales pipeline (config)

Leads follow a tenant **sales pipeline** (`pipelineId`), not an industry type.

- Default pipeline code: `DEFAULT_SALES_V1` (created on first use per tenant)
- Stages/transitions are stored in `sales_pipeline*` tables
- Stage codes currently align with `LeadStatus` so existing journey commands keep working
- Create lead without `pipelineId` → tenant default pipeline is assigned
- No `industry` field on Lead (industry/business domain lives on Tenant / later `businessDomainId`)

Default stage graph:

`NEW → CONTACTED → QUALIFIED → APPOINTMENT_BOOKED → VISITED → NEGOTIATING → WON | LOST → ARCHIVED`

Sales language aliases: `APPOINTMENT_BOOKED` ≈ visit scheduled, `VISITED` ≈ visit completed, `WON` ≈ booked/converted.

- **Validate** (`validated=true`) is required before **assign**
- Prefer journey commands over generic status PATCH
- Terminal: `WON`, `LOST`, `ARCHIVED` (archive only from WON/LOST)
- AI never writes lead rows — it returns qualification/score results consumed via commands

## APIs (`/api/v1/pipelines`)

| Method | Path | Description |
|--------|------|-------------|
| GET | `/` | List active tenant pipelines |
| GET | `/default` | Get or create `DEFAULT_SALES_V1` |
| GET | `/{pipelineId}` | Get pipeline (stages + transitions) |

## APIs (`/api/v1/leads`)

| Method | Path | Description |
|--------|------|-------------|
| POST | `/` | Create (publishes `LeadCreated`) |
| GET | `/` | Search (`status`, `assignedTo`, `sourceType`, `q`) |
| GET | `/{id}` | Get |
| PATCH | `/{id}` | Update details |
| DELETE | `/{id}` | Soft delete |
| POST | `/{id}/validate` | Validate |
| POST | `/{id}/assign` | Assign (`MANUAL` or `ROUND_ROBIN`) |
| POST | `/{id}/qualify` | Qualify (+ optional score) |
| POST | `/{id}/contact` | Mark contacted |
| POST | `/{id}/status` | Explicit status transition |
| POST | `/{id}/score` | Score |
| POST | `/{id}/convert` | Convert → WON (booked) |
| POST | `/{id}/lose` | Mark LOST |
| POST | `/{id}/schedule-visit` | Schedule site visit |
| POST | `/{id}/complete-visit` | Complete site visit |
| POST | `/{id}/cancel-visit` | Cancel scheduled visit → QUALIFIED |
| POST | `/{id}/archive` | Archive WON/LOST |
| GET | `/{id}/timeline` | Append-only journey timeline (includes conversation projections) |
| GET/POST | `/{id}/notes` | Notes |
| GET | `/{id}/activities` | Activities |
| GET/POST | `/{id}/followups` | Follow-ups |
| GET | `/{id}/history` | Status history |
| GET | `/duplicates` | Duplicate detections |
| POST | `/{id}/duplicates/{duplicateId}/resolve` | Resolve duplicate |
| GET/POST | `/{id}/attachments` | Attachment metadata (binary via media-service) |
| GET/POST | `/custom-fields` | Tenant custom field definitions |
| GET/POST | `/{id}/attributions` | Marketing attribution |
| GET/POST | `/{id}/quality-scores` | AI quality scores |
| GET/PUT | `/assignment-pool` | Round-robin assignee pool |

Requires JWT with `tenantId` from identity-service.

## Assignment strategies

- `MANUAL` (default): requires `assignedTo`
- `ROUND_ROBIN`: picks least-recently-assigned enabled member from `/assignment-pool`

## Conversation timeline (Phase 2)

Lead-service consumes `ConversationStarted` / `ConversationMessageAdded` / `ConversationClosed`
and appends `LeadActivity` rows (`CONVERSATION_*`). Message bodies stay in conversation-service.

## Lead → Customer conversion

`POST /api/v1/leads/{id}/convert`

- If `customerId` is provided → validated via customer-service Feign
- If omitted → creates (or reuses by `sourceLeadId`) a customer with
  `sourceType=LEAD_CONVERSION`
- Local Feign URL: `aisales.clients.customer-service.url` (default `http://localhost:8084`)

## Workflow

`workflow-service` coordinates `LEAD_LIFECYCLE_V1` from Kafka:
`LeadCreated` → validate → qualify → `LeadAssigned` → `WorkflowCompleted`.
Business decisions stay in lead-service.

## Events (outbox → Kafka)

`LeadCreated`, `LeadValidated`, `LeadQualified`, `LeadAssigned`, `LeadContacted`,
`LeadScored`, `LeadConverted`, `LeadLost`, `LeadStatusChanged`,
`LeadVisitScheduled`, `LeadVisitCompleted`, `LeadVisitCancelled`, `LeadArchived`
