#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"

SERVICES=(
  infrastructure/service-registry
  infrastructure/config-server
  infrastructure/api-gateway
  backend/services/identity-service
  backend/services/tenant-service
)

for svc in "${SERVICES[@]}"; do
  echo "Building Docker image for $svc"
  module="${svc#backend/}"
  ./scripts/build-service.sh "$module"
  docker build -t "aisales/${svc##*/}:latest" "$svc"
done
