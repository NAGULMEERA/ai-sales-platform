#!/usr/bin/env bash
# Restore PostgreSQL dumps created by backup-postgres.sh
# Usage:
#   PGHOST=localhost PGUSER=aisales PGPASSWORD=*** ./scripts/restore-postgres.sh ./backups/postgres/<stamp>
set -euo pipefail

IN_DIR="${1:?Usage: $0 <backup-directory>}"
PGHOST="${PGHOST:-localhost}"
PGPORT="${PGPORT:-5432}"
PGUSER="${PGUSER:-aisales}"

if [[ ! -d "${IN_DIR}" ]]; then
  echo "Backup directory not found: ${IN_DIR}" >&2
  exit 1
fi

echo "WARNING: This overwrites target databases with dumps from ${IN_DIR}"
echo "Press Ctrl+C within 5 seconds to abort..."
sleep 5

for dump in "${IN_DIR}"/*.dump; do
  [[ -f "${dump}" ]] || continue
  db="$(basename "${dump}" .dump)"
  echo "Restoring ${db}..."
  psql -h "${PGHOST}" -p "${PGPORT}" -U "${PGUSER}" -d postgres -v ON_ERROR_STOP=1 \
    -c "SELECT pg_terminate_backend(pid) FROM pg_stat_activity WHERE datname='${db}' AND pid <> pg_backend_pid();" \
    || true
  psql -h "${PGHOST}" -p "${PGPORT}" -U "${PGUSER}" -d postgres -v ON_ERROR_STOP=1 \
    -c "DROP DATABASE IF EXISTS ${db};"
  psql -h "${PGHOST}" -p "${PGPORT}" -U "${PGUSER}" -d postgres -v ON_ERROR_STOP=1 \
    -c "CREATE DATABASE ${db};"
  pg_restore -h "${PGHOST}" -p "${PGPORT}" -U "${PGUSER}" -d "${db}" --clean --if-exists --no-owner "${dump}"
done

echo "Restore complete from ${IN_DIR}"
