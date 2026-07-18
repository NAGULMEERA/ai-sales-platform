package com.aisales.integration.infrastructure.voice;

import com.aisales.common.observability.http.CorrelationIdPropagationInterceptor;
import com.aisales.integration.domain.voice.VoiceProvider;
import com.aisales.integration.infrastructure.configuration.VoiceProperties;
import com.fasterxml.jackson.databind.JsonNode;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
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
 * Twilio Programmable Voice outbound calls.
 * Selected when {@code aisales.integration.voice.provider=TWILIO}
 * and {@code aisales.integration.voice.twilio.enabled=true}.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "aisales.integration.voice.twilio.enabled", havingValue = "true")
public class TwilioVoiceProvider implements VoiceProvider {

    public static final String NAME = "TWILIO";

    private static final CorrelationIdPropagationInterceptor CORRELATION_ID_INTERCEPTOR =
            new CorrelationIdPropagationInterceptor();

    private final VoiceProperties properties;
    private final RestClient.Builder restClientBuilder;

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public VoiceCallResult placeOutboundCall(VoiceCallRequest request) {
        VoiceProperties.Twilio twilio = properties.getTwilio();
        if (!StringUtils.hasText(twilio.getAccountSid())
                || !StringUtils.hasText(twilio.getAuthToken())
                || !StringUtils.hasText(twilio.getFromNumber())) {
            return VoiceCallResult.failed(null, "Twilio accountSid/authToken/fromNumber not configured");
        }
        if (!StringUtils.hasText(request.toPhone())) {
            return VoiceCallResult.failed(null, "Lead phone is required for outbound call");
        }

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("To", request.toPhone());
        form.add("From", twilio.getFromNumber());
        if (StringUtils.hasText(twilio.getTwimlUrl())) {
            form.add("Url", twilio.getTwimlUrl());
        } else {
            form.add("Twiml", buildQualificationTwiml(request.customerName()));
        }
        if (StringUtils.hasText(twilio.getStatusCallbackUrl())) {
            form.add("StatusCallback", twilio.getStatusCallbackUrl());
            form.add("StatusCallbackEvent", "initiated");
            form.add("StatusCallbackEvent", "ringing");
            form.add("StatusCallbackEvent", "answered");
            form.add("StatusCallbackEvent", "completed");
        }

        String basic = Base64.getEncoder()
                .encodeToString((twilio.getAccountSid() + ":" + twilio.getAuthToken())
                        .getBytes(StandardCharsets.UTF_8));

        RestClient client = restClientBuilder.clone()
                .baseUrl(twilio.getBaseUrl())
                .defaultHeader("Authorization", "Basic " + basic)
                .requestFactory(ClientHttpRequestFactoryBuilder.detect()
                        .build(HttpClientSettings.defaults()
                                .withConnectTimeout(java.time.Duration.ofMillis(twilio.getConnectTimeoutMs()))
                                .withReadTimeout(java.time.Duration.ofMillis(twilio.getReadTimeoutMs()))))
                .requestInterceptor(CORRELATION_ID_INTERCEPTOR)
                .build();

        try {
            JsonNode body = client.post()
                    .uri("/2010-04-01/Accounts/{accountSid}/Calls.json", twilio.getAccountSid())
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .body(JsonNode.class);
            if (body == null) {
                return VoiceCallResult.failed(null, "Empty Twilio Calls response");
            }
            String sid = body.path("sid").asText(null);
            String status = body.path("status").asText("queued");
            if (!StringUtils.hasText(sid)) {
                return VoiceCallResult.failed(null, "Twilio response missing sid");
            }
            log.info("Twilio outbound call placed lead={} sid={} status={}", request.leadId(), sid, status);
            return VoiceCallResult.queued(sid);
        } catch (RestClientException ex) {
            log.warn("Twilio outbound call failed lead={}: {}", request.leadId(), ex.getMessage());
            return VoiceCallResult.failed(null, ex.getMessage());
        }
    }

    static String buildQualificationTwiml(String customerName) {
        String name = StringUtils.hasText(customerName) ? customerName : "there";
        return """
                <Response>
                  <Say voice="alice">Hello %s. This is your AI sales assistant calling about your enquiry.</Say>
                  <Pause length="1"/>
                  <Say voice="alice">Please share your preferred location, budget, and when you plan to buy. A specialist will follow up shortly. Thank you.</Say>
                </Response>
                """.formatted(escapeXml(name));
    }

    private static String escapeXml(String value) {
        return value.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }
}
