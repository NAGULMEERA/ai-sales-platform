# API Reference

Base URL (via gateway): `http://localhost:8080/api/v1`

## Authentication

| Method | Path | Description |
|--------|------|-------------|
| POST | /auth/login | Login with email/password |
| POST | /auth/refresh | Refresh access token |

## Tenants

| Method | Path | Description |
|--------|------|-------------|
| POST | /tenants | Create tenant |
| GET | /tenants/{id} | Get tenant by ID |
| GET | /tenants/slug/{slug} | Get tenant by slug |
| PUT | /tenants/{id} | Update tenant |
| DELETE | /tenants/{id} | Soft-delete tenant |

## Users

| Method | Path | Description |
|--------|------|-------------|
| POST | /users | Create user (requires X-Tenant-Id) |
| GET | /users/{id} | Get user by ID |

All responses use the standard `ApiResponse<T>` envelope with correlation ID.
