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

## Status machine

`NEW → CONTACTED → QUALIFIED → APPOINTMENT_BOOKED → VISITED → NEGOTIATING → WON | LOST`

- **Validate** (`validated=true`) is required before **assign**
- Terminal states: `WON`, `LOST`

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
| POST | `/{id}/convert` | Convert → WON |
| POST | `/{id}/lose` | Mark LOST |
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

## Workflow

`workflow-service` coordinates `LEAD_LIFECYCLE_V1` from Kafka:
`LeadCreated` → validate → qualify → `LeadAssigned` → `WorkflowCompleted`.
Business decisions stay in lead-service.

## Events (outbox → Kafka)

`LeadCreated`, `LeadValidated`, `LeadQualified`, `LeadAssigned`, `LeadContacted`,
`LeadScored`, `LeadConverted`, `LeadLost`, `LeadStatusChanged`
