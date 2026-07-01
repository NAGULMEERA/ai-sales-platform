#!/usr/bin/env bash
set -euo pipefail
SERVICE="${1:-identity-service}"
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../backend" && pwd)"
cd "$ROOT"
./mvnw -pl "services/$SERVICE" springdoc-openapi:generate -DskipTests || \
  echo "OpenAPI generation requires springdoc plugin configured per service"
