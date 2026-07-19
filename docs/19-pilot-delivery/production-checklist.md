# Production Checklist

Pilot production gate. Extends (does not replace) DevOps production docs.

## Core references

- [production-deployment.md](../11-devops/production-deployment.md)
- [kubernetes-production-checklist.md](../11-devops/kubernetes-production-checklist.md)
- [rolling-deploy-and-flyway.md](../11-devops/rolling-deploy-and-flyway.md)
- [blue-green-deployment.md](../11-devops/blue-green-deployment.md)

## Checklist

### Platform

- [ ] Helm release `aisales-platform` (or approved Kustomize) in target namespace
- [ ] RollingUpdate + HPA + PDB + NetworkPolicies applied
- [ ] Liveness/readiness/startup probes verified per service
- [ ] Graceful shutdown (`server.shutdown=graceful`) confirmed
- [ ] Resource requests/limits set; JVM `MaxRAMPercentage` in containers

### Data

- [ ] Flyway migrations reviewed; no destructive DDL in release
- [ ] Per-service DB credentials least-privilege
- [ ] Backup schedule active ([backup-checklist.md](./backup-checklist.md))
- [ ] Restore drill scheduled within 30 days of pilot start

### Messaging & cache

- [ ] Kafka ACLs / network isolation as designed
- [ ] Outbox publishers running; lag dashboards green
- [ ] Redis persistence / eviction policy documented for rate-limit keys

### Edge

- [ ] TLS terminated at Ingress; HTTP redirect
- [ ] Gateway JWT validation + rate limits enabled
- [ ] Webhook signature secrets rotated and stored in secret manager

### AI

- [ ] Real LLM + embedding providers; STUB blocked by prod guards
- [ ] Token/cost quotas and alerts configured
- [ ] Prompt injection rejection observed in logs/metrics on test payload

### Quality gates

- [ ] CI green (`mvn test` / verify) on release tag
- [ ] Security scan (Trivy/OWASP) reviewed; criticals waived or fixed
- [ ] Known Limitations reviewed with customer ([known-limitations.md](./known-limitations.md))

**Exit:** Signed production checklist + pilot deployment checklist complete.
