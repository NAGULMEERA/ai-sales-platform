# API Reference

Base URL (via gateway): `http://localhost:8080/api/v1`

For the full implemented surface (all routed services and controllers), see **[../08-api/api-surface.md](../08-api/api-surface.md)**.

Checked-in OpenAPI YAML (generate others via `scripts/generate-openapi.sh` + springdoc):

| Service | Spec |
|---------|------|
| identity-service | [../08-api/identity-service-openapi.yaml](../08-api/identity-service-openapi.yaml) |
| tenant-service | [../08-api/tenant-service-openapi.yaml](../08-api/tenant-service-openapi.yaml) |
| lead-service | [../08-api/lead-service-openapi.yaml](../08-api/lead-service-openapi.yaml) |

Runtime Swagger UI is available per service when running locally (e.g. `/swagger-ui.html` on the service port).

## Authentication (summary)

| Method | Path | Description |
|--------|------|-------------|
| POST | `/auth/login` | Login with email/password |
| POST | `/auth/refresh` | Refresh access token |
| POST | `/auth/register` | Registration (rate-limited at gateway) |

## Tenants (summary)

| Method | Path | Description |
|--------|------|-------------|
| POST | `/tenants` | Create tenant |
| GET | `/tenants/{id}` | Get tenant by ID |
| GET | `/tenants/slug/{slug}` | Get tenant by slug |
| PUT | `/tenants/{id}` | Update tenant |
| DELETE | `/tenants/{id}` | Soft-delete tenant |

## Users (summary)

| Method | Path | Description |
|--------|------|-------------|
| POST | `/users` | Create user |
| GET | `/users/{id}` | Get user by ID |

All responses use the standard `ApiResponse<T>` envelope with correlation ID. Error shape: [error-code-catalog.md](error-code-catalog.md).
