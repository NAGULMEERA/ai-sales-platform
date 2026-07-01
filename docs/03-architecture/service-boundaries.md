# Service Boundaries

| Service | Responsibility | Database |
|---------|----------------|----------|
| identity-service | Authentication, JWT, refresh tokens | identity_db |
| tenant-service | Tenants, tenant users, roles, permissions | tenant_db |
| lead-service | Lead capture, scoring, qualification | lead_db |
| customer-service | Customer profiles, merge | customer_db |
| catalog-service | Products and pricing | catalog_db |
| conversation-service | Chat sessions | conversation_db |
| appointment-service | Scheduling | appointment_db |
| ai-service | LLM orchestration | ai_db |
| workflow-service | Business process automation | workflow_db |
| notification-service | Email, SMS, push | notification_db |
| billing-service | Subscriptions and payments | billing_db |
| integration-service | External CRM/ERP connectors | integration_db |
| analytics-service | Reporting and dashboards | analytics_db |
| search-service | Full-text and semantic search projections | search_db |
| media-service | Media metadata (binary in S3) | media_db |
| audit-service | Centralized audit trail | audit_db |
| deal-service | Opportunities and sales outcomes | deal_db |
| marketplace-service | Plugin discovery and installation | marketplace_db |
