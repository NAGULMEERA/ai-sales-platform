# Identity Service



Platform authentication and authorization microservice (`identity-service`, port **8081**).



> **Full technical guide:** [docs/AUTHENTICATION.md](docs/AUTHENTICATION.md) — JWT timing, email verification, OAuth, refresh tokens, flows, and troubleshooting.



Per [service boundaries](../../docs/03-architecture/service-boundaries.md), this module owns:



- Email/password registration and login

- JWT access tokens and opaque refresh tokens

- Google OAuth2 login

- RBAC (roles and permissions in JWT)

- Multi-tenant context (`tenant_id`, `organization_id`)

- Tenant onboarding (register creates tenant + admin user)

- Subscription tiers (FREE / PREMIUM) and feature checks

- Email verification and password reset **tokens**

- Logout and session management

- Audit logging



**Email delivery** is owned by **notification-service** (port **8090**). identity-service calls it via REST.



## Quick start (Postman / local dev)



Use the **`local`** profile — no Kafka, Eureka, or Config Server required.



```powershell

# Option A: one script (starts Postgres + notification-service + identity-service)

.\scripts\run-identity-local.ps1



# Option B: manual steps

docker compose -f deployment/docker-compose-infra.yml up -d postgres



# Terminal 1 — notification-service (emails logged to console)

cd backend

.\mvnw.cmd spring-boot:run -pl services/notification-service -Dspring-boot.run.profiles=local



# Terminal 2 — identity-service

cd backend

.\mvnw.cmd spring-boot:run -pl services/identity-service -Dspring-boot.run.profiles=local

```



Swagger UI: http://localhost:8081/swagger-ui.html



Import Postman collection: `postman/identity-service.postman_collection.json`



### Suggested test order



1. **Register** — no JWT by default; `emailVerificationRequired: true`

2. Get verification **token** from notification-service logs or DB

3. **Verify Email** (POST or GET link)

4. **Login** — JWT created here; Postman saves tokens

5. **List Sessions** → **Get Subscription** → **Refresh** → **Logout**



See [AUTHENTICATION.md §14](docs/AUTHENTICATION.md#14-local-development-and-postman) for details.



### When is JWT created?



| Event | JWT? |

|-------|------|

| Register | No (default) |

| Verify email | No |

| **Login** | **Yes** |

| Refresh | Yes (new pair) |

| Google OAuth | Yes |



Full table: [AUTHENTICATION.md §3.4](docs/AUTHENTICATION.md#34-when-jwt-is-created).



### Troubleshooting



| Problem | Fix |

|---------|-----|

| Connection refused on 8081 | identity-service not running |

| Connection refused on 8090 | Start notification-service |

| Login: email verification required | Run **Verify Email** first |

| No email in inbox (local) | Check notification-service console (log mode) |

| Flyway / DB error | Postgres on **5433**; create `aisales_identity` / `aisales_notification` |

| Password authentication failed | Use Docker Postgres on **5433**, not local 5432 |

| Register fails on Kafka | Use `-Dspring-boot.run.profiles=local` |

| 401 on protected APIs | **Login** after verify; header `Authorization: Bearer {{accessToken}}` |

| Zipkin connection refused | Safe to ignore with `local` profile (tracing disabled) |



## API overview



| Method | Path | Auth | Description |

|--------|------|------|-------------|

| POST | `/api/v1/auth/register` | Public | Register tenant + admin (tokens only if auto-login enabled) |

| POST | `/api/v1/auth/login` | Public | Email/password login (requires verified email) |

| POST | `/api/v1/auth/refresh` | Public | Refresh tokens |

| POST | `/api/v1/auth/logout` | JWT | Revoke refresh token |

| POST | `/api/v1/auth/verify-email` | Public | Verify email token |

| GET | `/api/v1/auth/verify-email?token=` | Public | Verify email from link |

| POST | `/api/v1/auth/resend-verification` | Public | Resend verification email |

| POST | `/api/v1/auth/forgot-password` | Public | Send reset email |

| POST | `/api/v1/auth/reset-password` | Public | Reset password |

| GET | `/api/v1/auth/sessions` | JWT | List active sessions |

| GET | `/oauth2/authorization/google` | Public | Google OAuth2 |

| GET | `/api/v1/subscriptions/current` | JWT | Current plan |

| POST | `/api/v1/subscriptions/upgrade` | JWT | Upgrade to Premium |

| GET | `/api/v1/features/{code}` | JWT | Feature gate check |



## Configuration



| Property | Default | Description |

|----------|---------|-------------|

| `aisales.security.jwt.secret` | dev secret | JWT signing key (min 256 bits) |

| `aisales.auth.auto-login-after-register` | `false` | Issue JWT on register |

| `aisales.auth.require-email-verification-for-login` | `true` | Block login until verified |

| `aisales.notification.base-url` | `http://localhost:8090` | notification-service URL |

| `GOOGLE_CLIENT_ID` | change-me | Google OAuth client ID |

| `GOOGLE_CLIENT_SECRET` | change-me | Google OAuth secret |



## Database



Flyway migrations in `src/main/resources/db/migration/` (V1–V8):



- Users, roles, refresh tokens

- Tenants, RBAC permissions

- Sessions, email verification, password reset

- OAuth accounts

- Subscriptions, features, audit logs



## Tests



```bash

./mvnw test -pl services/identity-service

./mvnw test -pl services/notification-service

```



## Postman



Import `postman/identity-service.postman_collection.json`.



## Phase 2 (planned)



- Keycloak / OIDC federation

- MFA (TOTP)

- API keys for integrations

- SMTP production delivery (set notification `delivery-mode: smtp`)

- Payment webhook handlers (Stripe/Razorpay)


