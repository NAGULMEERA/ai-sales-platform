#!/usr/bin/env python3
"""Copy V015 conversation migration into conversation-service production split."""

from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
src = ROOT / "backend/database/migrations-monolith/V015__conversation.sql"
dst_dir = ROOT / "backend/database/service-splits/conversation-service"
dst_dir.mkdir(parents=True, exist_ok=True)

foundation = """-- Conversation Service: foundation subset
CREATE EXTENSION IF NOT EXISTS pgcrypto;
CREATE EXTENSION IF NOT EXISTS vector;
"""

if src.exists():
    tables_sql = src.read_text(encoding="utf-8")
    # Strip monolith header comments for service file
    idx = tables_sql.find("CREATE TABLE")
    body = tables_sql[idx:] if idx > 0 else tables_sql
    (dst_dir / "V001__foundation.sql").write_text(foundation, encoding="utf-8")
    (dst_dir / "V002__conversation_tables.sql").write_text(
        "-- From DB.html v7 section 4\n" + body, encoding="utf-8"
    )
    print(f"Conversation service split -> {dst_dir}")
else:
    raise SystemExit("V015__conversation.sql not found — run generate-migrations-from-db-html.py first")
