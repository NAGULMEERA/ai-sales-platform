# Blue/Green Deployment Strategy

## Intent

Switch production traffic between two identical application colors (`blue` / `green`) without changing business logic or public APIs.

## Mechanism

Implemented in Helm chart `deployment/helm/aisales-platform`:

- Each enabled service gets Deployments `{name}-blue` and `{name}-green` when `global.deploymentStrategy=blueGreen`.
- The stable Service selects pods with `app={name}` **and** `color={global.activeColor}`.
- Ingress always targets `api-gateway` Service (color-aware).

Raw Kubernetes manifests under `deployment/kubernetes/` remain **rolling-update** by default. Use Helm for blue/green.

## Procedure

1. **Baseline** — active color = `blue`, tag = `vN`.
2. **Deploy candidate** — keep `activeColor=blue` and set `global.colorImageTags.green=vN+1` (blue stays on `vN`).
3. **Validate green** — port-forward or internal smoke against green pods (`kubectl -n aisales get pods -l color=green`).
4. **Cut over** — `helm upgrade --set global.activeColor=green` (Service selector flips).
5. **Observe** — error rate, latency, Kafka lag, Flyway version table.
6. **Rollback** — flip `activeColor` back to `blue` (instant traffic switch).

## Flyway note

Schema migrations must be backward-compatible across both colors during the overlap window (expand/contract). Destructive migrations require a staged release — see [rolling-deploy-and-flyway.md](rolling-deploy-and-flyway.md).

## CI

`deploy-staging.yml` and `deploy-prod.yml` accept `strategy=blueGreen` and production `active_color`.
