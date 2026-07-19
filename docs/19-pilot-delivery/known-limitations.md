# Known Limitations (Pilot)

Honest constraints for design-partner pilots. Do not contradict these in sales materials.

## Security / compliance

| Limitation | Impact | Source |
|------------|--------|--------|
| No enterprise IdP / OIDC federation yet | Platform JWT login only | security-audit-report |
| Audit-service immutable store incomplete | Limited compliance export | security-audit-report |
| No upload antivirus | Rely on content-type/extension/magic-byte checks | security-audit-report |
| Field-level PII encryption at rest incomplete | Disk/volume encryption still required | security-audit-report |

**GA blocked** until IdP federation, audit store, and AV are complete. **Pilot allowed** with mitigations.

## Product / UX

| Limitation | Impact |
|------------|--------|
| No full SPA agent workspace guaranteed in pilot | API-first onboarding may apply |
| OpenAPI YAML only for identity, tenant, lead today | Other APIs via surface map + Swagger annotations |
| Industry inventory is attribute-level (`stockKg`) | Not a full WMS |
| Delivery tracking is conversation/timeline metadata | Not a logistics TMS |
| Appointment / audit services largely scaffold | Not pilot-critical paths |

## Platform

| Limitation | Impact |
|------------|--------|
| Cross-service tenant isolation ITs still expanding | Manual smoke required at go-live |
| Coverage gate 85% optional (`-Pcoverage-gate`) | Default CI reports coverage, does not fail scaffolds |
| Multi-region / active-active not offered | Single-region pilot |
| White-labeling / mobile apps not in scope | |

## Operational

| Limitation | Impact |
|------------|--------|
| Coordinated multi-DB PITR is process-heavy | Follow DR ordered restore |
| Redis used for rate limits/cache — treat as ephemeral | |

## Verticals available

- Real Estate, Automobile (validation complete)  
- Natural Farming (production metadata plugin)  
- Healthcare / Education / Insurance: not shipped

Update this file when residual risks close.
