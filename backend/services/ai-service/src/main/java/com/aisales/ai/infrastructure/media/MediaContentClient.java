package com.aisales.ai.infrastructure.media;

import com.aisales.common.core.constant.ApiConstants;
import com.aisales.common.core.util.TenantContext;
import com.aisales.common.exception.exception.BusinessException;
import com.aisales.common.exception.exception.NotFoundException;
import com.aisales.common.exception.exception.ValidationException;
import com.aisales.common.exception.model.ErrorCode;
import com.aisales.common.observability.http.CorrelationIdPropagationInterceptor;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

/**
 * Downloads tenant-scoped media bytes from Media Service for RAG ingest.
 */
@Component
@RequiredArgsConstructor
public class MediaContentClient {

    private static final CorrelationIdPropagationInterceptor CORRELATION_ID_INTERCEPTOR =
            new CorrelationIdPropagationInterceptor();

    private final RestClient.Builder restClientBuilder;

    @Value("${aisales.clients.media-service.url:http://localhost:8095}")
    private String mediaServiceBaseUrl;

    public MediaBinary download(UUID mediaId) {
        RestClient client = restClientBuilder.clone()
                .baseUrl(mediaServiceBaseUrl)
                .requestInterceptor(CORRELATION_ID_INTERCEPTOR)
                .requestInterceptor((request, body, execution) -> {
                    String tenantId = TenantContext.getTenantId();
                    if (StringUtils.hasText(tenantId)) {
                        request.getHeaders().set(ApiConstants.TENANT_ID_HEADER, tenantId);
                    }
                    return execution.execute(request, body);
                })
                .build();

        try {
            JsonNode metaResponse = client.get()
                    .uri("/api/v1/media/objects/{id}", mediaId)
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(JsonNode.class);
            JsonNode data = metaResponse != null ? metaResponse.path("data") : null;
            if (data == null || data.isMissingNode()) {
                throw new BusinessException(ErrorCode.AI_UNAVAILABLE, "Media metadata response missing data");
            }
            String contentType = textOrNull(data, "contentType");
            String filename = textOrNull(data, "originalFilename");

            byte[] bytes = client.get()
                    .uri("/api/v1/media/objects/{id}/content", mediaId)
                    .accept(MediaType.APPLICATION_OCTET_STREAM, MediaType.ALL)
                    .retrieve()
                    .body(byte[].class);
            if (bytes == null || bytes.length == 0) {
                throw new ValidationException("Media content is empty: " + mediaId);
            }
            return new MediaBinary(bytes, contentType, filename);
        } catch (RestClientResponseException ex) {
            if (ex.getStatusCode().value() == 404) {
                throw new NotFoundException("Media object not found: " + mediaId);
            }
            throw new BusinessException(
                    ErrorCode.AI_UNAVAILABLE,
                    "Failed to download media " + mediaId + ": " + ex.getMessage());
        }
    }

    private static String textOrNull(JsonNode node, String field) {
        JsonNode value = node.path(field);
        return value.isMissingNode() || value.isNull() ? null : value.asText();
    }

    public record MediaBinary(byte[] content, String contentType, String filename) {
    }
}
