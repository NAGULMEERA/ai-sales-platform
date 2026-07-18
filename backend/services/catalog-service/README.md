# catalog-service

Owns the **Catalog** bounded context: industry-agnostic products, services, priced offers,
and deterministic matching foundation.

Industry specifics (property, vehicle, course) belong in plugins via `attributes` JSONB
or plugin schemas — never as Platform Core tables.

## Local run

```powershell
docker compose -f deployment/docker-compose-infra.yml up -d postgres kafka
# CREATE DATABASE catalog_db;  (or use deployment/postgres/init-databases.sql)
cd backend
.\mvnw.cmd spring-boot:run -pl services/catalog-service "-Dspring-boot.run.profiles=local"
```

- Port: **8085**
- Swagger: http://localhost:8085/swagger-ui.html
- DB: `catalog_db` on Postgres **5433**

## Model

| Aggregate | Purpose |
|-----------|---------|
| `CatalogProduct` | Sellable PRODUCT or SERVICE (`code`, `category`, `attributes`) |
| `CatalogOffer` | Priced offer for a product (`currency`, `unitPrice`, validity) |
| Match API | Deterministic filter/score — not AI, not industry-specific |

## APIs

| Method | Path | Description |
|--------|------|-------------|
| POST | `/api/v1/products` | Create product/service |
| GET | `/api/v1/products` | Search (`status`, `productType`, `category`, `q`) |
| GET | `/api/v1/products/{id}` | Get product |
| PUT | `/api/v1/products/{id}` | Update product |
| DELETE | `/api/v1/products/{id}` | Soft-delete / archive |
| POST | `/api/v1/offers` | Create priced offer |
| GET | `/api/v1/offers` | List offers |
| GET | `/api/v1/offers/{id}` | Get offer |
| GET | `/api/v1/products/{productId}/offers` | Offers for product |
| PUT | `/api/v1/offers/{id}` | Update offer |
| DELETE | `/api/v1/offers/{id}` | Soft-delete / archive |
| POST | `/api/v1/matches` | Match products/offers to criteria |

Requires JWT with `tenantId` from identity-service.

## Events (outbox → Kafka)

`CatalogProductCreated`, `CatalogOfferCreated`

## Feign client

`com.aisales.common.contracts.client.CatalogServiceClient`
