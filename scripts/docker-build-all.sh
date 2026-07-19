#!/usr/bin/env bash
# Build all platform container images (multi-stage). Context = repository root.
set -euo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"

TAG="${IMAGE_TAG:-latest}"
REGISTRY="${IMAGE_REGISTRY:-aisales}"

SERVICES=(
  "backend/services/identity-service:identity-service"
  "backend/services/tenant-service:tenant-service"
  "backend/services/lead-service:lead-service"
  "backend/services/customer-service:customer-service"
  "backend/services/catalog-service:catalog-service"
  "backend/services/conversation-service:conversation-service"
  "backend/services/appointment-service:appointment-service"
  "backend/services/ai-service:ai-service"
  "backend/services/workflow-service:workflow-service"
  "backend/services/notification-service:notification-service"
  "backend/services/billing-service:billing-service"
  "backend/services/integration-service:integration-service"
  "backend/services/analytics-service:analytics-service"
  "backend/services/search-service:search-service"
  "backend/services/media-service:media-service"
  "backend/services/audit-service:audit-service"
  "backend/services/deal-service:deal-service"
  "backend/services/marketplace-service:marketplace-service"
  "infrastructure/api-gateway:api-gateway"
  "infrastructure/config-server:config-server"
  "infrastructure/service-registry:service-registry"
  "infrastructure/discovery-service:discovery-service"
)

for entry in "${SERVICES[@]}"; do
  path="${entry%%:*}"
  name="${entry##*:}"
  echo "==> Building ${REGISTRY}/${name}:${TAG} from ${path}/Dockerfile"
  docker build \
    -f "${path}/Dockerfile" \
    -t "${REGISTRY}/${name}:${TAG}" \
    .
done

echo "All images built with tag ${TAG}."
