#!/usr/bin/env bash
# Backup all AI Sales Platform PostgreSQL databases.
# Usage:
#   PGHOST=localhost PGPORT=5432 PGUSER=aisales PGPASSWORD=*** ./scripts/backup-postgres.sh [output-dir]
set -euo pipefail

OUT_DIR="${1:-./backups/postgres/$(date -u +%Y%m%dT%H%M%SZ)}"
PGHOST="${PGHOST:-localhost}"
PGPORT="${PGPORT:-5432}"
PGUSER="${PGUSER:-aisales}"

DATABASES=(
  aisales_identity
  aisales_tenant
  aisales_notification
  aisales_workflow
  lead_db
  catalog_db
  customer_db
  conversation_db
  deal_db
  ai_db
  marketplace_db
  # Additional service DBs if present in the target environment:
  appointment_db
  media_db
  billing_db
  search_db
  analytics_db
  audit_db
  integration_db
)

mkdir -p "${OUT_DIR}"
echo "Backing up to ${OUT_DIR}"

for db in "${DATABASES[@]}"; do
  if psql -h "${PGHOST}" -p "${PGPORT}" -U "${PGUSER}" -d postgres -tAc "SELECT 1 FROM pg_database WHERE datname='${db}'" | grep -q 1; then
    echo "Dumping ${db}..."
    pg_dump -h "${PGHOST}" -p "${PGPORT}" -U "${PGUSER}" -d "${db}" -Fc -f "${OUT_DIR}/${db}.dump"
  else
    echo "Skipping missing database: ${db}"
  fi
done

echo "Writing manifest..."
{
  echo "created_at_utc=$(date -u +%Y-%m-%dT%H:%M:%SZ)"
  echo "host=${PGHOST}:${PGPORT}"
  echo "user=${PGUSER}"
  ls -1 "${OUT_DIR}"/*.dump 2>/dev/null || true
} > "${OUT_DIR}/MANIFEST.txt"

echo "Backup complete: ${OUT_DIR}"
