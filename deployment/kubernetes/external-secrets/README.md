# Production secrets (Vault / Secret Manager)

## Rule

- **Secrets** live in Vault (or AWS/GCP Secret Manager).
- **Config Server** holds non-secret config only.
- **Pods** receive secrets as environment variables from the Kubernetes `Secret` `aisales-secrets`.

```text
Vault / Secret Manager
        ↓ External Secrets Operator
Kubernetes Secret (aisales-secrets)
        ↓ env / envFrom
Spring Boot services
```

## Apply (example)

1. Install [External Secrets Operator](https://external-secrets.io/).
2. Configure Vault KV path `secret/data/aisales/platform` and Kubernetes auth role `aisales-eso`.
3. Edit `vault-aisales-secrets.yaml` (`server`, paths, SA) for your environment.
4. `kubectl apply -f vault-aisales-secrets.yaml`
5. Deploy services — they already reference `aisales-secrets`.

For bootstrap without Vault, apply `../secrets.yml` with temporary placeholders, then replace with ESO.

## Local development

Do **not** use Vault. Use:

- `application-local.yml` + optional gitignored `application-local-secrets.yml`
- Docker Mailpit for SMTP, or local SMTP env vars
