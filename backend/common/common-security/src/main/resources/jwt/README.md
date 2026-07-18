# Local / test RSA keys (DEV ONLY)

`local-private.pem` and `local-public.pem` are committed **development** keys so local
services and integration tests can validate RS256 JWTs without Vault.

Never use these keys in production. Production identity must load a private key from
Secret Manager / Vault (`JWT_PRIVATE_KEY_PEM` or a file path), and other services should
validate via JWKS (`aisales.security.jwt.jwk-set-uri`).
