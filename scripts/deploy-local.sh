#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"

docker network create aisales-network 2>/dev/null || true
docker compose -f docker-compose-infra.yml up -d
sleep 10
docker compose -f docker-compose-services.yml up -d --build

echo "Platform deployed locally. Gateway: http://localhost:8080"
