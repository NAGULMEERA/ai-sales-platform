#!/usr/bin/env bash
set -euo pipefail
SERVICE="${1:?Usage: build-service.sh <module-path>}"
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../backend" && pwd)"
cd "$ROOT"
./mvnw clean package -pl "$SERVICE" -am -DskipTests
