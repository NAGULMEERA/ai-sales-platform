package com.aisales.common.observability.http;

import com.aisales.common.core.constant.ApiConstants;
import com.aisales.common.core.util.CorrelationIdUtils;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.util.StringUtils;

import java.io.IOException;

/**
 * Propagates the current request's {@code correlation_id} (already in MDC, set by
 * {@link com.aisales.common.observability.filter.CorrelationIdFilter}) onto every outbound
 * internal service-to-service HTTP call.
 *
 * <p>Unlike {@code traceId}/{@code spanId}, which Micrometer Tracing/Brave automatically propagate
 * on any {@code RestTemplate} built via the auto-configured {@code RestTemplateBuilder} (and on
 * {@code RestClient} via the equivalent observation instrumentation), {@code X-Correlation-Id} is
 * an application-specific header that Spring has no built-in knowledge of. Without this
 * interceptor, a downstream service receiving an internal call would find no correlation header
 * and mint a brand-new one via its own {@code CorrelationIdFilter}, silently breaking the ability
 * to search logs for a single logical request across service boundaries (Rule 08: "never lose
 * traceability").
 *
 * <p>Register on every {@code RestTemplate}/{@code RestClient.Builder} bean used for internal
 * (same-platform) calls. Deliberately not applied to external provider clients (e.g. Google
 * OAuth2, OpenAI): those providers have no use for our internal correlation id and forwarding
 * internal headers to third parties is unnecessary information disclosure.
 */
public class CorrelationIdPropagationInterceptor implements ClientHttpRequestInterceptor {

    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution)
            throws IOException {
        String correlationId = CorrelationIdUtils.getCorrelationId();
        if (StringUtils.hasText(correlationId) && !request.getHeaders().containsKey(ApiConstants.CORRELATION_ID_HEADER)) {
            request.getHeaders().add(ApiConstants.CORRELATION_ID_HEADER, correlationId);
        }
        return execution.execute(request, body);
    }
}
