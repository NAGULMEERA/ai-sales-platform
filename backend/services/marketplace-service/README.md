# marketplace-service

Owns the **plugin catalog and tenant installations** (thin registry).

Plugins are **metadata/config** — this service does not execute industry business logic,
load plugin jars dynamically, or send WhatsApp/email.

## Local run

```powershell
docker compose -f deployment/docker-compose-infra.yml up -d postgres kafka
# CREATE DATABASE marketplace_db;
cd backend
.\mvnw.cmd spring-boot:run -pl services/marketplace-service "-Dspring-boot.run.profiles=local"
```

- Port: **8098**
- Swagger: http://localhost:8098/swagger-ui.html
- DB: `marketplace_db` on Postgres **5433**

## Seeded catalog (Phase 5)

| pluginKey | type | notes |
|-----------|------|-------|
| `email-channel` | CAPABILITY | Config schema only; SMTP stays in notification-service |
| `whatsapp-channel` | CAPABILITY | Config schema only; Meta API not implemented here |
| `real-estate` | INDUSTRY | Attribute/pipeline metadata; no industry microservice |

## APIs

| Method | Path | Description |
|--------|------|-------------|
| GET | `/api/v1/marketplace/plugins` | List catalog (`type` filter) |
| GET | `/api/v1/marketplace/plugins/{pluginKey}` | Get catalog entry |
| GET | `/api/v1/marketplace/installations` | Tenant installations |
| POST | `/api/v1/marketplace/plugins/{pluginKey}/enable` | Enable + optional config |
| POST | `/api/v1/marketplace/plugins/{pluginKey}/disable` | Disable |
| PUT | `/api/v1/marketplace/installations/{id}/config` | Update config |

## Events

`PluginEnabled`, `PluginDisabled`

## Feign

`com.aisales.common.contracts.client.MarketplaceServiceClient`

## SDK

Author plugins against `backend/plugins/plugin-sdk` (`PluginDescriptor`, `CapabilityPlugin`, `IndustryPlugin`).
