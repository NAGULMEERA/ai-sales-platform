# C4 Level 1 — System Context

Grounded in the current repository layout (`backend/services`, `infrastructure/api-gateway`, `deployment/`).

```mermaid
C4Context
    title AI Sales Platform — System Context

    Person(agent, "Sales Agent / Admin", "Uses JWT-authenticated APIs via the gateway")
    Person(customer, "End Customer", "Interacts via WhatsApp/voice/webhooks when integrations are enabled")

    System(platform, "AI Sales Platform", "Multi-tenant lead-to-deal SaaS with AI assist, search, analytics, workflows")

    System_Ext(llm, "LLM Providers", "OpenAI / Gemini / Stub behind ai-service ports")
    System_Ext(meta, "Meta / Twilio", "Lead ads and voice webhooks via integration-service")
    System_Ext(stripe, "Stripe", "Payment webhooks via billing-service")
    System_Ext(s3, "Object Storage (S3)", "Media binaries; metadata in media-service")
    System_Ext(mail, "SMTP / Mailpit", "Transactional email via notification-service")

    Rel(agent, platform, "HTTPS /api/v1/**", "JWT Bearer")
    Rel(customer, meta, "Messages / calls")
    Rel(meta, platform, "Signed webhooks")
    Rel(stripe, platform, "Signed webhooks")
    Rel(platform, llm, "Provider SDKs inside ai-service only")
    Rel(platform, s3, "Pre-signed upload/download")
    Rel(platform, mail, "SMTP")
```

## Notes (implemented)

- External entry is **API Gateway** (`infrastructure/api-gateway`), not individual service ports in production paths.
- AI vendors are never called from Lead/Deal/Workflow services; only `ai-service` owns provider adapters.
- Appointment and audit services are currently health-only scaffolds; gateway still routes `/api/v1/appointments/**` and `/api/v1/audit/**`.

## Related

- [c4-containers.md](c4-containers.md)
- [service-boundaries.md](service-boundaries.md)
- [../15-adr/adr-009-api-gateway.md](../15-adr/adr-009-api-gateway.md)
