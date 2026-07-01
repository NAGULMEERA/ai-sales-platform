#!/usr/bin/env python3
"""Generate Flyway migrations from DB.html v7.0 (AI-Native) logical schema.

Source: .cursor/knowledge/Documents/DB.html
Target: database/migrations-monolith/ (lab monolith; V015+ and gap fills)

DDS.html (V001-V014, V028) remains executable SQL authority — do not overwrite those files.
"""

from __future__ import annotations

import re
from datetime import date
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
DB_HTML = ROOT / ".cursor/knowledge/Documents/DB.html"
OUT_DIR = ROOT / "backend/database/migrations-monolith"
LINEAGE = ROOT / "backend/database/db-html-v7-lineage.md"

TYPE_MAP = {
    "UUID": "UUID",
    "TEXT": "TEXT",
    "JSONB": "JSONB",
    "BOOLEAN": "BOOLEAN",
    "INTEGER": "INTEGER",
    "BIGINT": "BIGINT",
    "BIGSERIAL": "BIGSERIAL",
    "DATE": "DATE",
    "TIMESTAMPTZ": "TIMESTAMPTZ",
    "INET": "INET",
    "DECIMAL": "DECIMAL(19,4)",
    "TEXT[]": "TEXT[]",
}

# Flyway files generated from DB.html (never overwrite DDS-sourced V001-V014, V028)
MIGRATION_SPECS: list[tuple[str, str, list[str]]] = [
    ("V015__conversation.sql", "Conversations and AI memory (§4)", [r"^4\."]),
    ("V016__integration.sql", "Integration and tenant migration (§20-21)", [r"^20\.", r"^21\."]),
    ("V017__billing.sql", "Billing and cost protection (§8-9)", [r"^8\.", r"^9\."]),
    ("V018__analytics.sql", "Analytics and deals (§18)", [r"^18\."]),
    ("V019__audit.sql", "Audit and data governance (§16-17)", [r"^16\.", r"^17\."]),
    ("V020__observability.sql", "AI observability (§26)", [r"^26\."]),
    ("V021__plugin.sql", "Plugin lifecycle (§19)", [r"^19\."]),
    ("V022__marketplace.sql", "Feature marketplace (§13)", [r"^13\."]),
    ("V024__eventing.sql", "Eventing outbox/inbox/DLQ (§15.1,15.3,15.4)", [r"^15\.1", r"^15\.3", r"^15\.4"]),
    ("V025__reliability.sql", "Reliability idempotency/circuit breaker (§15.2,15.5,15.6)", [r"^15\.2", r"^15\.5", r"^15\.6"]),
    ("V030__human_agent.sql", "Human agent workspace (§12)", [r"^12\."]),
    ("V031__ml_model_registry.sql", "AI/ML model registry and MLOps (§22)", [r"^22\."]),
    ("V032__agent_orchestration_gap.sql", "Agent orchestration gap vs DDS (§25)", [r"^25\."]),
]

# Tables already created by DDS.html migrations — skip in §25 gap file
DDS_EXISTING_TABLES = {
    "tool_registry",
    "agent_errors",
    "agent_memories",
    "semantic_cache",
    "cache_hits",
    "cache_invalidations",
    "rag_pipelines",
    "chunking_strategies",
    "embedding_models",
    "ingestion_jobs",
    "retrieval_logs",
}

TABLE_PATTERN = re.compile(
    r"<h3>(?P<section>\d+\.\d+)\s+(?P<name>[a-z0-9_]+)</h3>\s*"
    r"<div class=\"table-wrap\">\s*<table>.*?"
    r"<thead><tr><th>Column</th>.*?</thead>\s*<tbody>(?P<body>.*?)</tbody>",
    re.DOTALL | re.IGNORECASE,
)

ROW_PATTERN = re.compile(
    r"<tr><td><code>(?P<col>[a-z0-9_]+)</code></td><td>(?P<type>[^<]+)</td>",
    re.IGNORECASE,
)

INDEX_PATTERN = re.compile(
    r"<strong>Indexes:</strong>\s*(?P<indexes>.*?)(?:<br|<p>|<div class=\"highlight|<strong>Check|<strong>Partitioning)",
    re.DOTALL | re.IGNORECASE,
)


def normalize_type(raw: str) -> str:
    raw = raw.strip()
    if raw.startswith("vector"):
        return raw
    if raw.startswith("DECIMAL("):
        return raw
    return TYPE_MAP.get(raw, "TEXT")


def column_sql(name: str, col_type: str) -> str:
    t = normalize_type(col_type)
    if name == "id" and t == "UUID":
        return "    id UUID PRIMARY KEY DEFAULT gen_random_uuid()"
    if name == "id" and t == "BIGSERIAL":
        return "    id BIGSERIAL PRIMARY KEY"
    if name == "key" and t == "TEXT":
        return "    key TEXT PRIMARY KEY"
    if name == "event_id" and t == "TEXT":
        return "    event_id TEXT PRIMARY KEY"
    if name == "tenant_id" and t == "UUID":
        return "    tenant_id UUID NOT NULL"
    if t == "JSONB":
        return f"    {name} JSONB DEFAULT '{{}}'::jsonb"
    if name == "created_at" and t == "TIMESTAMPTZ":
        return "    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()"
    if name == "updated_at" and t == "TIMESTAMPTZ":
        return "    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()"
    if t == "BOOLEAN":
        return f"    {name} BOOLEAN DEFAULT false"
    return f"    {name} {t}"


def strip_html(text: str) -> str:
    return re.sub(r"<[^>]+>", "", text).strip()


def parse_indexes(block: str, table_name: str) -> list[str]:
    m = INDEX_PATTERN.search(block)
    if not m:
        return []
    text = re.sub(r"\s+", " ", m.group("indexes"))
    statements: list[str] = []
    for part in re.split(r",\s*(?=idx_)", text):
        part = part.strip().rstrip(",")
        if not part or part.upper().startswith("PRIMARY KEY") or part.upper().startswith("CONSTRAINT"):
            continue
        m2 = re.match(r"(idx_[a-z0-9_]+)\s*\(([^)]+)\)(.*)", part, re.IGNORECASE)
        if not m2:
            continue
        idx_name, cols, tail = m2.group(1), m2.group(2), m2.group(3)
        if "NOW()" in tail.upper():
            statements.append(f"-- skipped (non-immutable): {idx_name} on {table_name}")
            continue
        if "WHERE" in tail.upper():
            where = strip_html(tail[tail.upper().find("WHERE") :])
            statements.append(
                f"CREATE INDEX IF NOT EXISTS {idx_name} ON {table_name}({cols}) {where};"
            )
        else:
            statements.append(f"CREATE INDEX IF NOT EXISTS {idx_name} ON {table_name}({cols});")
    return statements


def parse_tables(html: str) -> dict[str, dict]:
    tables: dict[str, dict] = {}
    for m in TABLE_PATTERN.finditer(html):
        section = m.group("section")
        name = m.group("name")
        body = m.group("body")
        if "sample-data" in body[:80]:
            continue
        cols = [(row.group("col"), row.group("type")) for row in ROW_PATTERN.finditer(body)]
        if not cols:
            continue
        start = m.start()
        end = html.find("<h3>", m.end())
        block = html[start : end if end != -1 else start + 5000]
        tables[f"{section} {name}"] = {
            "section": section,
            "name": name,
            "columns": cols,
            "indexes": parse_indexes(block, name),
        }
    return tables


def table_matches(section: str, patterns: list[str]) -> bool:
    return any(re.match(p, section) for p in patterns)


def render_table(t: dict, if_not_exists: bool = False) -> str:
    create_kw = "CREATE TABLE IF NOT EXISTS" if if_not_exists else "CREATE TABLE"
    lines = [f"-- DB.html {t['section']} {t['name']}", f"{create_kw} {t['name']} ("]
    lines.append(",\n".join(column_sql(c, ty) for c, ty in t["columns"]))
    lines.append(");")
    lines.append("")
    lines.extend(t["indexes"])
    lines.append("")
    return "\n".join(lines)


def write_migration(filename: str, title: str, tables: list[dict], if_not_exists: bool = False) -> None:
    header = f"""-- ============================================================================
-- {filename}
-- Generated from DB.html v7.0: {title}
-- Lab monolith only. Split per service for production (UUID refs, no cross-FK).
-- ============================================================================

"""
    body = "\n".join(render_table(t, if_not_exists=if_not_exists) for t in tables)
    path = OUT_DIR / filename
    path.write_text(header + body, encoding="utf-8")
    print(f"Wrote {filename} ({len(tables)} tables)")


def write_scheduler() -> None:
    sql = """-- ============================================================================
-- V023__scheduler.sql
-- Scheduled jobs (synthesized — no dedicated DB.html section)
-- ============================================================================

CREATE TABLE IF NOT EXISTS scheduled_jobs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID,
    job_name VARCHAR(255) NOT NULL,
    job_type VARCHAR(100) NOT NULL,
    cron_expression VARCHAR(100) NOT NULL,
    payload JSONB DEFAULT '{}'::jsonb,
    enabled BOOLEAN NOT NULL DEFAULT true,
    last_run_at TIMESTAMPTZ,
    next_run_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    metadata JSONB DEFAULT '{}'::jsonb
);

CREATE INDEX IF NOT EXISTS idx_scheduled_jobs_tenant ON scheduled_jobs(tenant_id);
CREATE INDEX IF NOT EXISTS idx_scheduled_jobs_next_run ON scheduled_jobs(next_run_at) WHERE enabled = true;

CREATE TABLE IF NOT EXISTS job_executions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    job_id UUID NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'RUNNING',
    started_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    completed_at TIMESTAMPTZ,
    error_message TEXT,
    result JSONB DEFAULT '{}'::jsonb,
    metadata JSONB DEFAULT '{}'::jsonb
);

CREATE INDEX IF NOT EXISTS idx_job_executions_job ON job_executions(job_id, started_at DESC);
CREATE INDEX IF NOT EXISTS idx_job_executions_status ON job_executions(status);

ALTER TABLE job_executions DROP CONSTRAINT IF EXISTS fk_job_executions_job;
ALTER TABLE job_executions ADD CONSTRAINT fk_job_executions_job
    FOREIGN KEY (job_id) REFERENCES scheduled_jobs(id) ON DELETE CASCADE;
"""
    (OUT_DIR / "V023__scheduler.sql").write_text(sql, encoding="utf-8")
    print("Wrote V023__scheduler.sql")


def write_views() -> None:
    sql = """-- ============================================================================
-- V026__views.sql
-- Operational views (DB.html v7 + DDS-compatible filters)
-- ============================================================================

CREATE OR REPLACE VIEW v_lead_funnel AS
SELECT tenant_id, status, COUNT(*) AS lead_count, DATE_TRUNC('day', created_at) AS day
FROM leads
WHERE status NOT IN ('WON', 'LOST')
GROUP BY tenant_id, status, DATE_TRUNC('day', created_at);

CREATE OR REPLACE VIEW v_tenant_active_users AS
SELECT tenant_id, COUNT(*) AS active_users
FROM users
WHERE status = 'ACTIVE'
GROUP BY tenant_id;

CREATE OR REPLACE VIEW v_pending_notifications AS
SELECT tenant_id, channel, COUNT(*) AS pending_count
FROM notifications
WHERE status = 'PENDING'
GROUP BY tenant_id, channel;
"""
    (OUT_DIR / "V026__views.sql").write_text(sql, encoding="utf-8")
    print("Wrote V026__views.sql")


def write_materialized_views() -> None:
    sql = """-- ============================================================================
-- V027__materialized_views.sql
-- DB.html v7 section 27
-- ============================================================================

CREATE MATERIALIZED VIEW IF NOT EXISTS mv_lead_funnel_daily AS
SELECT tenant_id, status, COUNT(*) AS lead_count, DATE(created_at) AS metric_date
FROM leads
WHERE status NOT IN ('WON', 'LOST')
GROUP BY tenant_id, status, DATE(created_at);

CREATE UNIQUE INDEX IF NOT EXISTS idx_mv_lead_funnel_daily
    ON mv_lead_funnel_daily(tenant_id, status, metric_date);

CREATE MATERIALIZED VIEW IF NOT EXISTS mv_tenant_daily_metrics AS
SELECT tenant_id, DATE(created_at) AS metric_date,
    COUNT(*) FILTER (WHERE status = 'NEW') AS new_leads,
    COUNT(*) FILTER (WHERE status = 'QUALIFIED') AS qualified_leads,
    COUNT(*) FILTER (WHERE status = 'WON') AS won_leads
FROM leads
GROUP BY tenant_id, DATE(created_at);

CREATE UNIQUE INDEX IF NOT EXISTS idx_mv_tenant_daily_metrics
    ON mv_tenant_daily_metrics(tenant_id, metric_date);

CREATE MATERIALIZED VIEW IF NOT EXISTS mv_ai_cost_daily AS
SELECT tenant_id, date AS metric_date, SUM(total_cost) AS total_cost, SUM(total_tokens) AS total_tokens
FROM ai_cost_metrics
GROUP BY tenant_id, date;

CREATE UNIQUE INDEX IF NOT EXISTS idx_mv_ai_cost_daily
    ON mv_ai_cost_daily(tenant_id, metric_date);

CREATE MATERIALIZED VIEW IF NOT EXISTS mv_ai_quality_daily AS
SELECT tenant_id, date AS metric_date, AVG(accuracy) AS avg_accuracy, AVG(hallucination_rate) AS avg_hallucination_rate
FROM ai_quality_metrics
GROUP BY tenant_id, date;

CREATE UNIQUE INDEX IF NOT EXISTS idx_mv_ai_quality_daily
    ON mv_ai_quality_daily(tenant_id, metric_date);
"""
    (OUT_DIR / "V027__materialized_views.sql").write_text(sql, encoding="utf-8")
    print("Wrote V027__materialized_views.sql")


def write_seed_data() -> None:
    sql = """-- ============================================================================
-- V029__seed_data.sql
-- Reference seed data (DB.html v7 sample alignment)
-- ============================================================================

INSERT INTO permissions (id, code, name, description, category)
SELECT gen_random_uuid(), r.code, r.name, r.description, r.category
FROM (VALUES
    ('lead:read', 'Read leads', 'Read lead records', 'lead'),
    ('lead:create', 'Create leads', 'Create lead records', 'lead'),
    ('lead:update', 'Update leads', 'Update lead records', 'lead'),
    ('lead:delete', 'Delete leads', 'Delete lead records', 'lead'),
    ('customer:read', 'Read customers', 'Read customer records', 'customer'),
    ('appointment:read', 'Read appointments', 'Read appointment records', 'appointment')
) AS r(code, name, description, category)
WHERE NOT EXISTS (SELECT 1 FROM permissions p WHERE p.code = r.code);

INSERT INTO plans (id, name, price_monthly, price_annual, features, limits, trial_days)
SELECT gen_random_uuid(), p.name, p.price_monthly, p.price_annual, p.features::jsonb, p.limits::jsonb, p.trial_days
FROM (VALUES
    ('Starter', 2999.00, 29990.00, '{"leads": 100}', '{"users": 5}', 14),
    ('Pro', 5999.00, 59990.00, '{"leads": 500}', '{"users": 25}', 14),
    ('Enterprise', 12999.00, 129990.00, '{"leads": "unlimited"}', '{"users": "unlimited"}', 7)
) AS p(name, price_monthly, price_annual, features, limits, trial_days)
WHERE NOT EXISTS (SELECT 1 FROM plans pl WHERE pl.name = p.name);
"""
    (OUT_DIR / "V029__seed_data.sql").write_text(sql, encoding="utf-8")
    print("Wrote V029__seed_data.sql")


def write_lineage(parsed: dict[str, dict]) -> None:
    sections: dict[str, list[str]] = {}
    for t in parsed.values():
        major = t["section"].split(".")[0]
        sections.setdefault(major, []).append(t["name"])

    lines = [
        "# DB.html v7.0 Schema Lineage",
        "",
        f"Generated: {date.today().isoformat()}",
        "",
        "## Source of truth",
        "",
        "| Artifact | Role |",
        "|----------|------|",
        "| `.cursor/knowledge/Documents/DB.html` | **Logical schema v7.0** (160+ tables, indexes, samples) |",
        "| `.cursor/knowledge/Documents/DDS.html` | **Executable SQL** for V001–V014, V028 only |",
        "| `database/migrations-monolith/V001–V014,V028` | From **DDS.html** (with architect FK fixes) |",
        "| `database/migrations-monolith/V015+` | From **DB.html v7** via generator |",
        "",
        f"## Parsed tables from DB.html: **{len(parsed)}**",
        "",
    ]
    for major in sorted(sections, key=int):
        names = ", ".join(sorted(sections[major]))
        lines.append(f"- **§{major}** ({len(sections[major])}): {names}")
    lines.extend([
        "",
        "## Regenerate",
        "",
        "```bash",
        "python scripts/generate-migrations-from-db-html.py",
        "```",
        "",
        "## Known DDS vs DB.html differences (lab monolith uses DDS for core domains)",
        "",
        "| Area | DDS.html | DB.html v7 |",
        "|------|----------|------------|",
        "| leads | `customer_id`, audit columns, ENUM status | `customer_profile_id`, TEXT status |",
        "| tenants | `tenant_code`, ENUMs, extra tables | Simpler core columns + `tenant_ai_config` |",
        "| conversations | Not in DDS | §4 full model (BIGSERIAL ids) |",
        "| AI MLOps | V009–V012 different table names | §22–25 model_registry, experiments, etc. |",
        "",
        "Production split: align JPA to **service owner**; use DB.html for field completeness, DDS for DDL where available.",
        "",
    ])
    LINEAGE.write_text("\n".join(lines), encoding="utf-8")
    print(f"Wrote {LINEAGE.relative_to(ROOT)}")


def main() -> None:
    html = DB_HTML.read_text(encoding="utf-8")
    parsed = parse_tables(html)
    ordered = sorted(parsed.values(), key=lambda x: [int(p) for p in x["section"].split(".")])

    for filename, title, patterns in MIGRATION_SPECS:
        selected = [t for t in ordered if table_matches(t["section"], patterns)]
        if filename == "V032__agent_orchestration_gap.sql":
            selected = [t for t in selected if t["name"] not in DDS_EXISTING_TABLES]
            write_migration(filename, title, selected, if_not_exists=True)
        else:
            write_migration(filename, title, selected)

    write_scheduler()
    write_views()
    write_materialized_views()
    write_seed_data()
    write_lineage(parsed)
    print(f"Done. {len(parsed)} tables parsed from DB.html v7.0")


if __name__ == "__main__":
    main()
