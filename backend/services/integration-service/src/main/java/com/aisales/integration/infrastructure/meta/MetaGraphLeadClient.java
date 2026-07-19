package com.aisales.integration.infrastructure.meta;

import com.aisales.common.exception.exception.BusinessException;
import com.aisales.common.exception.exception.ValidationException;
import com.aisales.common.exception.model.ErrorCode;
import com.aisales.common.observability.http.CorrelationIdPropagationInterceptor;
import com.aisales.integration.application.dto.StubLeadAdsPayload;
import com.aisales.integration.infrastructure.configuration.MetaLeadAdsProperties;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.http.client.ClientHttpRequestFactoryBuilder;
import org.springframework.boot.http.client.HttpClientSettings;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Fetches a Lead Ads submission from Meta Graph API by {@code leadgen_id}.
 * HTTP runs outside any DB transaction.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MetaGraphLeadClient {

    private static final CorrelationIdPropagationInterceptor CORRELATION_ID_INTERCEPTOR =
            new CorrelationIdPropagationInterceptor();

    private final MetaLeadAdsProperties properties;
    private final RestClient.Builder restClientBuilder;

    public StubLeadAdsPayload fetchLead(String leadgenId, String pageId, String accessTokenOverride) {
        if (!StringUtils.hasText(leadgenId)) {
            throw new ValidationException("leadgenId is required for Graph fetch");
        }
        String token = StringUtils.hasText(accessTokenOverride)
                ? accessTokenOverride
                : properties.getGraph().getAccessToken();
        if (!StringUtils.hasText(token)) {
            throw new BusinessException(
                    ErrorCode.INTERNAL_ERROR,
                    "Meta Graph access token is not configured "
                            + "(aisales.integration.meta.lead-ads.graph.access-token)");
        }

        MetaLeadAdsProperties.Graph graph = properties.getGraph();
        String base = graph.getBaseUrl().endsWith("/")
                ? graph.getBaseUrl() + graph.getApiVersion()
                : graph.getBaseUrl() + "/" + graph.getApiVersion();

        RestClient client = restClientBuilder.clone()
                .baseUrl(base)
                .requestFactory(ClientHttpRequestFactoryBuilder.detect()
                        .build(HttpClientSettings.defaults()
                                .withConnectTimeout(java.time.Duration.ofMillis(graph.getConnectTimeoutMs()))
                                .withReadTimeout(java.time.Duration.ofMillis(graph.getReadTimeoutMs()))))
                .requestInterceptor(CORRELATION_ID_INTERCEPTOR)
                .build();

        try {
            JsonNode body = client.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/{leadgenId}")
                            .queryParam(
                                    "fields",
                                    "id,created_time,ad_id,ad_name,form_id,field_data,platform")
                            .queryParam("access_token", token)
                            .build(leadgenId))
                    .retrieve()
                    .body(JsonNode.class);
            if (body == null || body.isNull()) {
                throw new BusinessException(ErrorCode.INTERNAL_ERROR, "Empty Meta Graph lead response");
            }
            return mapLead(body, pageId, leadgenId);
        } catch (RestClientException ex) {
            log.warn("Meta Graph lead fetch failed leadgenId={}: {}", leadgenId, ex.getMessage());
            throw new BusinessException(
                    ErrorCode.INTERNAL_ERROR, "Meta Graph lead fetch failed: " + ex.getMessage());
        }
    }

    static StubLeadAdsPayload mapLead(JsonNode body, String pageId, String fallbackLeadgenId) {
        Map<String, String> flat = new HashMap<>();
        JsonNode fieldData = body.path("field_data");
        if (fieldData.isArray()) {
            for (JsonNode field : fieldData) {
                String name = normalizeKey(field.path("name").asText(""));
                String value = firstValue(field.path("values"));
                if (StringUtils.hasText(name) && StringUtils.hasText(value)) {
                    flat.put(name, value);
                }
            }
        }

        String fullName = firstNonBlank(
                flat.remove("full_name"),
                flat.remove("full name"),
                flat.remove("name"),
                flat.remove("fullname"));
        String phone = firstNonBlank(
                flat.remove("phone_number"),
                flat.remove("phone"),
                flat.remove("mobile"),
                flat.remove("mobile_number"));
        String email = firstNonBlank(flat.remove("email"), flat.remove("email_address"));

        Map<String, Object> fields = new HashMap<>();
        flat.forEach(fields::put);
        if (body.hasNonNull("ad_id")) {
            fields.put("metaAdId", body.path("ad_id").asText());
        }
        if (body.hasNonNull("form_id")) {
            fields.put("metaFormId", body.path("form_id").asText());
        }
        if (body.hasNonNull("ad_name")) {
            fields.put("metaAdName", body.path("ad_name").asText());
        }

        String platform = body.path("platform").asText(null);
        String campaign = body.path("ad_name").asText(null);

        return StubLeadAdsPayload.builder()
                .pageId(pageId)
                .leadgenId(body.path("id").asText(fallbackLeadgenId))
                .platform(platform)
                .fullName(fullName)
                .phone(phone)
                .email(email)
                .campaign(campaign)
                .fields(fields)
                .build();
    }

    private static String firstValue(JsonNode values) {
        if (values != null && values.isArray() && !values.isEmpty()) {
            return values.get(0).asText(null);
        }
        return null;
    }

    private static String normalizeKey(String raw) {
        if (!StringUtils.hasText(raw)) {
            return "";
        }
        return raw.trim().toLowerCase(Locale.ROOT).replace(' ', '_');
    }

    private static String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                return value.trim();
            }
        }
        return null;
    }
}
