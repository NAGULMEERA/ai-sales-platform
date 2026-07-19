# Admin Manual (Tenant Admin)

Day-2 operations for the pilot tenant administrator. Platform operators use DevOps/ops runbooks.

## 1. Access

- Login via gateway auth endpoints / approved client  
- Keep MFA process for admin accounts (even if IdP MFA is future)  
- Rotate passwords after first login

## 2. Users & roles

- Invite users through identity user APIs / admin UI when available  
- Assign least-privilege roles (agent vs admin)  
- Disable leavers promptly

## 3. Industry configuration

After Delivery enables your plugin:

| Vertical | Plugin key | Pipeline (typical) |
|----------|------------|--------------------|
| Natural Farming | `natural-farming` | `NATURAL_FARMING_SALES_V1` |
| Real Estate | `real-estate` | `REAL_ESTATE_SALES_V1` |
| Automobile | `automobile` | `AUTOMOBILE_SALES_V1` |

Do not invent custom stage **codes** outside `LeadStatus`; change display labels via plugin/pipeline templates only.

## 4. Catalog

- Create products and priced offers  
- Fill attribute keys for your industry (farm/harvest/stock for Natural Farming; bedrooms/location for RE; make/model for Auto)  
- Keep `stockKg` / availability updated if using attribute-level inventory

## 5. Leads & pipeline

- Capture leads (API, WhatsApp/Meta if configured)  
- Move stages per allowed transitions  
- Use AI qualification as advisory input

## 6. Deals

- Open opportunity → create quote from catalog offers  
- Accept quote → follow billing/payment instructions from Delivery

## 7. Knowledge / AI

- Upload approved FAQs only  
- Index documents; wait for READY status before relying on RAG  
- Monitor AI quota with your Delivery contact if limits hit

## 8. Search & analytics

- Search is eventually consistent after events  
- Dashboards are tenant-scoped; never share tokens across tenants

## 9. When to call support

Provide: time (UTC), tenant id, user email, `correlation_id`, steps to reproduce, screenshots/API payloads (redact secrets).

See [support-checklist.md](./support-checklist.md) and [known-limitations.md](./known-limitations.md).
