# customer-service

Owns the **Customer** bounded context: lifecycle, contact details, addresses, and
lead-conversion linkage. Industry-agnostic (no Party rewrite in Phase 1).

## Local run

```powershell
docker compose -f deployment/docker-compose-infra.yml up -d postgres kafka
# CREATE DATABASE customer_db;  (or use deployment/postgres/init-databases.sql)
cd backend
.\mvnw.cmd spring-boot:run -pl services/customer-service "-Dspring-boot.run.profiles=local"
```

- Port: **8084**
- Swagger: http://localhost:8084/swagger-ui.html
- DB: `customer_db` on Postgres **5433**

## Model

| Aggregate / entity | Purpose |
|--------------------|---------|
| `Customer` | System of record after lead conversion / import / manual capture |
| `CustomerAddress` | Postal addresses (`primaryAddress` flag) |

Status: `PROSPECT → ACTIVE → INACTIVE | ARCHIVED`

Source: `LEAD_CONVERSION` (requires `sourceLeadId`), `IMPORT`, `MANUAL`

Must have phone **or** email. Lead reference is a UUID only (no cross-service FK).

## APIs (`/api/v1/customers`)

| Method | Path | Description |
|--------|------|-------------|
| POST | `/` | Create |
| GET | `/` | Search (`status`, `q`) |
| GET | `/{id}` | Get |
| PUT | `/{id}` | Update |
| POST | `/{id}/activate` | Activate |
| POST | `/{id}/archive` | Archive |
| DELETE | `/{id}` | Soft-delete |
| GET/POST | `/{id}/addresses` | Addresses |
| PUT/DELETE | `/{id}/addresses/{addressId}` | Update / soft-delete address |

Requires JWT with `tenantId` from identity-service.

## Events (outbox → Kafka)

`CustomerCreated`, `CustomerUpdated`, `CustomerArchived`

## Feign client

`com.aisales.common.contracts.client.CustomerServiceClient` (typed `CustomerDto`)

## Lead conversion flow

Preferred (orchestrated by lead-service):

1. `POST /api/v1/leads/{id}/convert` with no `customerId`
2. lead-service Feign-creates customer (`LEAD_CONVERSION` + `sourceLeadId`)
3. Lead marked `WON` with linked `customerId`

Manual:

1. `POST /api/v1/customers` with `sourceType=LEAD_CONVERSION` + `sourceLeadId`
2. `POST /api/v1/leads/{id}/convert` with that `customerId`

Lookup: `GET /api/v1/customers/by-source-lead/{leadId}`
