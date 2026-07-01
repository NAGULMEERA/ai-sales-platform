# DATABASE-ARCHITECTURE.md

# AI Sales Platform Database Architecture
Version: 1.0 (Frozen)

## Purpose

This document defines the permanent architectural rules for the PostgreSQL database. All Flyway migrations and database changes must comply with this document.

---

# Technology Stack

- PostgreSQL 17+
- Flyway
- pgvector
- pgcrypto
- pg_trgm
- JSONB

---

# Multi-Tenancy

- Shared database
- Shared schemas
- Shared tables
- Row isolation using `tenant_id`
- Tenant isolation enforced in the application layer initially
- PostgreSQL RLS may be introduced later if required

---

# Schema Ownership

| Schema | Owner |
|--------|-------|
| core | Shared platform objects |
| tenant | Tenant management |
| identity | Authentication & authorization |
| crm | Leads, customers, opportunities |
| catalog | Products / properties |
| conversation | Conversations & messages |
| workflow | Workflow execution |
| appointment | Scheduling |
| notification | Notifications |
| analytics | Reporting |
| ai | AI platform objects |

Each schema owns only its own tables, views, functions, and sequences.

---

# Cross-Schema Rules

Allowed:
- CRM → Identity
- CRM → Catalog
- Workflow → CRM
- Analytics → Read from business schemas

Avoid direct business coupling with AI schemas. AI services should access business data through APIs or controlled integration points.

---

# AI Data Strategy

Business schemas own:
- Business records
- Embedding columns
- Business metadata

AI schema owns:
- Prompt templates
- Agent definitions
- AI request history
- Model registry
- Search configuration

---

# Audit Standard

Every business table includes:
- id
- tenant_id
- created_at
- created_by
- updated_at
- updated_by
- version
- deleted

---

# Database Standards

- UUID primary keys
- snake_case naming
- JSONB only for flexible metadata
- pgvector only for semantic search
- Flyway for all schema changes
- Forward-only migrations

---

# Migration Roadmap

1. V001 Baseline
2. V002 Extensions
3. V003 Schemas
4. V004 Common Objects
5. V005 Tenant
6. V006 Identity
7. V007 CRM
8. Remaining services

This document is considered frozen until an approved Architecture Decision Record (ADR) changes it.
