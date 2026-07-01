#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../backend" && pwd)"
cd "$ROOT"
./mvnw test "$@"
