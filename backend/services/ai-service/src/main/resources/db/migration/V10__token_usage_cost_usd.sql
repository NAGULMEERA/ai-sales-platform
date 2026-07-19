-- Track B: optional USD cost estimate on AI usage ledger (embeddings + LLM).

ALTER TABLE token_usage
    ADD COLUMN estimated_cost_usd NUMERIC(19, 8);

COMMENT ON COLUMN token_usage.estimated_cost_usd IS
    'Config-priced estimate only; not an invoice. Billing-service remains SoT for charges.';
