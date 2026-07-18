# deal-service

Owns the **Deal** bounded context: sales opportunities and commercial quotes.

Industry-specific deal fields belong in plugins later — Platform Core stays generic.

## Local run

```powershell
docker compose -f deployment/docker-compose-infra.yml up -d postgres kafka
# CREATE DATABASE deal_db;  (or use deployment/postgres/init-databases.sql)
cd backend
.\mvnw.cmd spring-boot:run -pl services/deal-service "-Dspring-boot.run.profiles=local"
```

- Port: **8097**
- Swagger: http://localhost:8097/swagger-ui.html
- DB: `deal_db` on Postgres **5433**
- Catalog (for quote pricing): http://localhost:8085

## Model

| Aggregate | Purpose |
|-----------|---------|
| `Opportunity` | Commercial opportunity linked to `customerId` (+ optional `leadId`) |
| `Quote` | Versioned quote with catalog offer snapshots |
| Assignment | Opportunity **owner** (`assignedTo`) — manual only |

Lead pool / ROUND_ROBIN stays in **lead-service**. Do not duplicate here.

## Status

Opportunity: `OPEN` → `QUOTED` → `WON` | `LOST` | `CANCELLED`

Quote: `DRAFT` → `SENT` → `ACCEPTED` | `REJECTED` | `SUPERSEDED`

## APIs

| Method | Path | Description |
|--------|------|-------------|
| POST | `/api/v1/opportunities` | Create opportunity |
| GET | `/api/v1/opportunities` | Search (`status`, `customerId`, `leadId`) |
| GET | `/api/v1/opportunities/{id}` | Get opportunity |
| PUT | `/api/v1/opportunities/{id}` | Update opportunity |
| POST | `/api/v1/opportunities/{id}/assign` | Assign owner (manual) |
| POST | `/api/v1/quotes` | Create quote (catalog offer snapshots) |
| GET | `/api/v1/quotes/{id}` | Get quote |
| GET | `/api/v1/opportunities/{opportunityId}/quotes` | List quotes |
| POST | `/api/v1/quotes/{id}/send` | Send draft → marks opportunity QUOTED |
| POST | `/api/v1/quotes/{id}/accept` | Accept sent → marks opportunity WON |

Requires JWT with `tenantId` from identity-service.

## Events (outbox → Kafka)

`OpportunityCreated`, `OpportunityAssigned`, `OpportunityStatusChanged`,
`QuoteCreated`, `QuoteSent`, `QuoteAccepted`

## Feign client

`com.aisales.common.contracts.client.DealServiceClient`
