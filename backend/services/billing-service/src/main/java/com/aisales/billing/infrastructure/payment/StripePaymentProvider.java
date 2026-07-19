package com.aisales.billing.infrastructure.payment;

import com.aisales.billing.domain.payment.PaymentProvider;
import com.aisales.billing.infrastructure.configuration.PaymentProperties;
import com.aisales.common.exception.exception.BusinessException;
import com.aisales.common.exception.model.ErrorCode;
import com.aisales.common.observability.http.CorrelationIdPropagationInterceptor;
import com.fasterxml.jackson.databind.JsonNode;
import java.math.BigDecimal;
import java.math.RoundingMode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.http.client.ClientHttpRequestFactoryBuilder;
import org.springframework.boot.http.client.HttpClientSettings;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Stripe PaymentIntents. Selected when {@code aisales.billing.payment.provider=STRIPE}
 * and {@code aisales.billing.payment.stripe.enabled=true}.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "aisales.billing.payment.stripe.enabled", havingValue = "true")
public class StripePaymentProvider implements PaymentProvider {

    public static final String NAME = "STRIPE";

    private static final CorrelationIdPropagationInterceptor CORRELATION_ID_INTERCEPTOR =
            new CorrelationIdPropagationInterceptor();

    private final PaymentProperties properties;
    private final RestClient.Builder restClientBuilder;

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public PaymentChargeResult charge(PaymentChargeRequest request) {
        PaymentProperties.Stripe config = properties.getStripe();
        if (!StringUtils.hasText(config.getApiKey())) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "Stripe API key is not configured");
        }

        long amountCents = request.amountUsd()
                .multiply(BigDecimal.valueOf(100))
                .setScale(0, RoundingMode.HALF_UP)
                .longValueExact();
        if (amountCents < 1) {
            return PaymentChargeResult.succeeded("stripe_zero_" + request.invoiceId());
        }

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("amount", Long.toString(amountCents));
        form.add("currency", request.currency().toLowerCase());
        form.add("metadata[invoice_id]", request.invoiceId().toString());
        form.add("metadata[tenant_id]", request.tenantId().toString());
        form.add("automatic_payment_methods[enabled]", "false");

        boolean autoConfirm = config.isAutoConfirm();
        String paymentMethod = StringUtils.hasText(request.paymentMethodId())
                ? request.paymentMethodId()
                : config.getDefaultPaymentMethod();
        if (autoConfirm && StringUtils.hasText(paymentMethod)) {
            form.add("confirm", "true");
            form.add("payment_method", paymentMethod);
            form.add("payment_method_types[0]", "card");
        } else {
            form.add("payment_method_types[0]", "card");
        }

        RestClient client = restClientBuilder.clone()
                .baseUrl(config.getBaseUrl())
                .defaultHeader("Authorization", "Bearer " + config.getApiKey())
                .requestFactory(ClientHttpRequestFactoryBuilder.detect()
                        .build(HttpClientSettings.defaults()
                                .withConnectTimeout(java.time.Duration.ofMillis(config.getConnectTimeoutMs()))
                                .withReadTimeout(java.time.Duration.ofMillis(config.getReadTimeoutMs()))))
                .requestInterceptor(CORRELATION_ID_INTERCEPTOR)
                .build();

        try {
            JsonNode body = client.post()
                    .uri("/v1/payment_intents")
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .body(JsonNode.class);
            if (body == null) {
                return PaymentChargeResult.failed(null, "Empty Stripe response");
            }
            String id = body.path("id").asText(null);
            String status = body.path("status").asText("");
            String clientSecret = body.path("client_secret").asText(null);
            if ("succeeded".equalsIgnoreCase(status)) {
                return PaymentChargeResult.succeeded(id);
            }
            if ("requires_action".equalsIgnoreCase(status)
                    || "requires_payment_method".equalsIgnoreCase(status)
                    || "requires_confirmation".equalsIgnoreCase(status)) {
                return PaymentChargeResult.pending(id, clientSecret);
            }
            return PaymentChargeResult.failed(id, "Stripe status=" + status);
        } catch (RestClientException ex) {
            log.warn("Stripe payment failed for invoice {}: {}", request.invoiceId(), ex.getMessage());
            return PaymentChargeResult.failed(null, ex.getMessage());
        }
    }
}
