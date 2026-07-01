# Pending migrations – status after Phase 3 generation

| Version | File | Status |
|---------|------|--------|
| V016 | integration | Generated from DB.html §20 |
| V017 | billing | Generated from DB.html §8–9 |
| V018 | analytics | Generated from DB.html §18 |
| V019 | audit | Generated from DB.html §16 |
| V020 | observability | Generated from DB.html §26 |
| V021 | plugin | Generated from DB.html §19 |
| V022 | marketplace | Generated from DB.html §13 |
| V023 | scheduler | Synthesized (jobs + executions) |
| V024 | eventing | Generated from DB.html §15.1, 15.3, 15.4 |
| V025 | reliability | Generated from DB.html §15.2, 15.5, 15.6 |
| V026 | views | Synthesized operational views |
| V027 | materialized_views | From DB.html §27 |
| V029 | seed_data | Lab reference seeds |

Regenerate V016–V025 tables:

```bash
python scripts/generate-migrations-from-db-html.py
```
