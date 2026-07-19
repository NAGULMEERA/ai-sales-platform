# Customer Onboarding Guide (Pilot)

Process for enabling a pilot tenant. Strategy background: [tenant-onboarding-strategy.md](../02-business/tenant-onboarding-strategy.md).

## Roles

| Role | Responsibility |
|------|----------------|
| Delivery Manager | Owns timeline, checklists, customer communication |
| Platform Admin | Creates tenant, enables plugins, seeds config |
| Tenant Admin (customer) | Invites users, configures day-2 settings |
| CS | Training, first-week health checks |

## Day 0 — Provision

1. Collect: company name, industry vertical, admin email, timezone, data residency notes  
2. Create tenant (`POST /api/v1/tenants`)  
3. Create admin user (identity) and share temporary credentials via secure channel  
4. Assign subscription/plan (billing) if billing in pilot scope  
5. Enable industry plugin:
   - Natural Farming: `POST /api/v1/plugins/natural-farming/enable`
   - Real Estate: `.../real-estate/enable`
   - Automobile: `.../automobile/enable`
6. Ensure sales pipeline for industry template code  
7. Confirm admin can login via gateway

## Day 1 — Configure

1. Invite additional users / roles  
2. Create catalog products/offers using plugin attribute keys  
3. Optional: WhatsApp / Meta webhook binding (integration-service)  
4. Optional: upload knowledge FAQ → index for RAG  
5. Configure notification from-addresses / WhatsApp phone number IDs if used  
6. Walk tenant admin through [admin-manual.md](./admin-manual.md)

## Day 2–7 — Activate

| Checkpoint | Pass |
|------------|------|
| ≥1 lead created | [ ] |
| ≥1 AI qualification or execute | [ ] |
| ≥1 catalog match or recommend | [ ] |
| ≥1 opportunity/quote (if sales in scope) | [ ] |
| Search finds seeded entities | [ ] |
| No Sev-1/2 open | [ ] |

## Handoff package to customer

- [ ] API usage guide + OpenAPI links (identity/tenant/lead)  
- [ ] Known Limitations  
- [ ] Support channel + SLA  
- [ ] Industry vertical doc (e.g. Natural Farming)

## Do not promise in pilot

- Enterprise IdP/SSO  
- Full immutable compliance audit warehouse  
- Upload antivirus  
- White-label UI (frontend may be API-only for pilot)
