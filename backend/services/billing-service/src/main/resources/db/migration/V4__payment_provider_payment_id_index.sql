-- Lookup payments by Stripe PaymentIntent id (webhook completion).
CREATE INDEX idx_payment_provider_payment_id
    ON payment (provider_payment_id)
    WHERE provider_payment_id IS NOT NULL;
