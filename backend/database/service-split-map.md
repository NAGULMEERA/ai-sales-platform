# DDS v2.1 Service Split Map

Maps monolith Flyway files to owning microservices for **production** deployment (database-per-service).

## Rules

1. **One database per microservice** – no shared schema ownership.
2. **Remove cross-service FKs** – keep `tenant_id`, `customer_id`, `user_id` as UUID columns only.
3. **V001 foundation** – each service gets a **subset** of extensions/ENUMs/functions it needs.
4. **V023 eventing** – every service gets its own `outbox` / `inbox` tables (not one shared migration).
5. **Industry tables** (builders, projects, properties) move to **real-estate-plugin**, not catalog-service core.

## Migration → Service

| Monolith File | Primary Service | Secondary / Notes |
|---------------|-----------------|-------------------|
| V001 foundation | All (subset) | `vector` only on ai, search, conversation |
| V002 security | identity-service | Strip `fk_*_tenant` → UUID ref |
| V003 tenant | tenant-service | Includes deferred tenant FKs for lab only |
| V004 lead | lead-service | No FK to customers/users |
| V005 customer | customer-service | |
| V006 catalog | catalog-service | Platform catalog only; RE tables → plugin |
| V007 appointment | appointment-service | |
| V008 workflow | workflow-service | |
| V009 ai_core | ai-service | |
| V010 ai_agents | ai-service | |
| V011 rag | ai-service | |
| V012 semantic_cache | ai-service | |
| V013 voice_ai | integration-service or voice plugin | |
| V014 notification | notification-service | |
| V015 conversation | conversation-service | From DB.html §4 |
| V016 integration | integration-service | From DB.html §20–21 |
| V017 billing | billing-service | From DB.html §8–9 |
| V018 analytics | analytics-service | From DB.html §18 |
| V019 audit | audit-service | From DB.html §16–17 |
| V020 observability | ai-service / observability | From DB.html §26 |
| V021 plugin | platform-service / registry | From DB.html §19 |
| V022 marketplace | marketplace-service | From DB.html §13 |
| V023 scheduler | platform job runner | Generated |
| V024 eventing | **each service** | Split outbox/inbox per service |
| V025 reliability | shared resilience metadata | From DB.html §15 |
| V026 views | analytics / read models | Generated |
| V027 materialized_views | analytics-service | Generated |
| V029 seed_data | `database/seed/{service}/` | Per-context DML |
| V030 human_agent | conversation-service or ops | From DB.html §12 |
| V031 ml_model_registry | ai-service | From DB.html §22 |
| V032 agent_orchestration_gap | ai-service | From DB.html §25 (IF NOT EXISTS vs DDS V010) |
| V028 advanced_indexes | Split by table owner | Remove duplicates |

## Cross-Service FK Removal (Production)

| Table | Column | Monolith FK | Production |
|-------|--------|-------------|------------|
| users | tenant_id | → tenants | UUID ref + event sync |
| leads | tenant_id | → tenants | UUID ref |
| leads | customer_id | → customers | UUID ref |
| lead_assignments | assigned_to | → users | UUID ref |
| tenant_members | user_id | → users | UUID ref (identity) |
| notifications | lead_id, user_id | — | UUID refs |

## First Service Split: lead-service

Production migrations in `backend/services/lead-service/src/main/resources/db/migration/`.

| File | Content |
|------|---------|
| V001__foundation.sql | `pgcrypto`, `lead_status` ENUM, lead helper functions |
| V002__lead_tables.sql | All V004 tables, no cross-service FKs |
| V003__lead_indexes.sql | Lead-specific indexes from V004 + V028 subset |

## Second Service Split: conversation-service

Production migrations in `backend/services/conversation-service/src/main/resources/db/migration/`.

| File | Content |
|------|---------|
| V001__foundation.sql | `pgcrypto`, `vector` |
| V002__conversation_tables.sql | DB.html §4 (8 tables, UUID refs to lead/customer) |

## Phase Checklist

- [x] Extract V001–V014, V028 from DDS.html
- [x] Fix V002/V003/V004/V005 FK order (lab)
- [x] Add V015 conversation (gap vs DB.html)
- [x] Generate V015–V032, V029 from DB.html v7
- [x] Promote lead-service migrations (V001–V003)
- [x] Promote conversation-service migrations (V001–V002)
- [ ] Run lab Flyway migrate (requires Docker Desktop)
- [ ] Split remaining services
