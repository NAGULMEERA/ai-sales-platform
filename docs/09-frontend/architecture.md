# Frontend Architecture

## Stack

| Layer | Technology |
|-------|------------|
| UI | React 18 + TypeScript |
| Components | MUI (Material UI) |
| Routing | React Router v6 |
| Server state | TanStack React Query |
| Client state | Zustand (persisted UI prefs) |
| HTTP | Axios with interceptors |

---

## Layered Architecture

```
Presentation (pages, features, MUI components)
        ↓
API Client (React Query + Axios)
        ↓
API Gateway (JWT, rate limit, tenant resolution)
        ↓
Microservices
```

---

## Directory Structure (planned)

```
frontend/
├── admin-portal/          # Tenant admin, billing, users
├── agent-workspace/       # Sales agent daily workflow
└── packages/
    ├── ui/                # Shared MUI theme + components
    └── api-client/        # Generated OpenAPI clients
```

---

## State Management

- **Auth, tenant, user** — Zustand store with secure token handling
- **Server data** — React Query (cache, retry, invalidation)
- **UI prefs** (theme, sidebar) — Zustand + localStorage persist

---

## API Client

- Attach `Authorization: Bearer {token}` and `X-Tenant-Id` on every request
- 401 → refresh token flow → logout on failure
- React Query defaults: 5 min stale time, 3 retries with exponential backoff

---

## Security

- PKCE OAuth2 flow for login (when Keycloak is deployed)
- Never store refresh token in localStorage (httpOnly cookie preferred)
- CSP headers enforced at CDN / gateway

---

## Related

- [oauth2-oidc-strategy.md](../10-security/oauth2-oidc-strategy.md)
- OpenAPI specs: `docs/08-api/`
