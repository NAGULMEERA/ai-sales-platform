# DB.html v7.0 Schema Lineage

Generated: 2026-06-27

## Source of truth

| Artifact | Role |
|----------|------|
| `.cursor/knowledge/Documents/DB.html` | **Logical schema v7.0** (160+ tables, indexes, samples) |
| `.cursor/knowledge/Documents/DDS.html` | **Executable SQL** for V001–V014, V028 only |
| `database/migrations-monolith/V001–V014,V028` | From **DDS.html** (with architect FK fixes) |
| `database/migrations-monolith/V015+` | From **DB.html v7** via generator |

## Parsed tables from DB.html: **151**

- **§1** (7): tenant_ai_config, tenant_features, tenants, user_permissions, user_roles, user_sessions, users
- **§2** (8): lead_assignments, lead_attribution, lead_duplicates, lead_notes, lead_scorecards, lead_status_history, lead_tags, leads
- **§3** (6): customer_identities, customer_interactions, customer_journey, customer_merge_history, customer_preferences, customer_profiles
- **§4** (8): ai_sessions, conversation_history, conversation_memory, conversation_sentiment, conversation_state, conversation_summaries, conversation_transfers, conversations
- **§5** (6): catalog_attribute_definitions, catalog_embeddings, catalog_images, catalog_items, catalog_status_history, catalogs
- **§6** (4): appointment_reminders, appointment_status_history, appointments, calendar_sync_logs
- **§7** (9): agent_execution_logs, agent_executions, saga_executions, saga_steps, workflow_analytics, workflow_definitions, workflow_executions, workflow_snapshots, workflow_versions
- **§8** (8): invoices, payments, plans, pricing_catalog, pricing_versions, subscriptions, usage_billing, usage_metering
- **§9** (3): cost_protection_actions, cost_protection_alerts, cost_protection_policies
- **§10** (8): chunking_strategies, document_chunks, document_embeddings, document_versions, documents, ingestion_jobs, knowledge_sources, rag_pipelines
- **§11** (5): call_events, call_recordings, call_sessions, call_transcripts, voice_call_logs
- **§12** (5): agent_activity, agent_assignments, agent_availability, agent_notes, agent_queue
- **§13** (3): marketplace_audit, marketplace_installations, marketplace_listings
- **§14** (12): ai_evaluations, ai_metrics, hallucination_logs, llm_requests, model_performance_metrics, prompt_drift_logs, prompt_evaluations, prompt_execution_logs, prompt_templates, prompt_versions, safety_checks, token_usage
- **§15** (6): circuit_breaker_state, dead_letter, idempotency_keys, outbox_events, processed_events, retry_logs
- **§16** (4): audit_hash_chain, audit_log, legal_hold_records, legal_holds
- **§17** (5): archival_logs, data_access_policies, data_archival_policies, data_classification, retention_policies
- **§18** (7): analytics_kpis, attribution_reports, conversion_funnels, opportunities, revenue_tracking, sales_outcomes, tenant_dashboards
- **§19** (4): plugin_audit, plugin_installations, plugin_registry, plugin_versions
- **§20** (5): contract_tests, oauth_tokens, provider_configs, webhook_logs, webhooks
- **§21** (2): tenant_migration_jobs, tenant_migration_logs
- **§22** (7): experiment_results, experiment_variants, experiments, fine_tuning_jobs, model_deployments, model_registry, model_versions
- **§23** (3): cache_hits, cache_invalidations, semantic_cache
- **§24** (4): chunking_strategies, embedding_models, rag_pipelines, rag_retrieval_logs
- **§25** (6): agent_errors, agent_feedback, agent_memory, agent_plans, tool_calls, tool_registry
- **§26** (6): ai_alerts, ai_cost_metrics, ai_drift_detection, ai_performance_metrics, ai_quality_metrics, prompt_drift_logs

## Regenerate

```bash
python scripts/generate-migrations-from-db-html.py
```

## Known DDS vs DB.html differences (lab monolith uses DDS for core domains)

| Area | DDS.html | DB.html v7 |
|------|----------|------------|
| leads | `customer_id`, audit columns, ENUM status | `customer_profile_id`, TEXT status |
| tenants | `tenant_code`, ENUMs, extra tables | Simpler core columns + `tenant_ai_config` |
| conversations | Not in DDS | §4 full model (BIGSERIAL ids) |
| AI MLOps | V009–V012 different table names | §22–25 model_registry, experiments, etc. |

Production split: align JPA to **service owner**; use DB.html for field completeness, DDS for DDL where available.
