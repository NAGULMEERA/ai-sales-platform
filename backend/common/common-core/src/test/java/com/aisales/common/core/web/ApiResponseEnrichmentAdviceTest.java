package com.aisales.common.core.web;

import com.aisales.common.core.constant.ApiConstants;
import com.aisales.common.core.dto.ApiResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;

class ApiResponseEnrichmentAdviceTest {

    private final ApiResponseEnrichmentAdvice advice = new ApiResponseEnrichmentAdvice();

    @Test
    void shouldEnrichApiResponseWithPathAndVersion() {
        ApiResponse<String> response = ApiResponse.ok("value");
        MockHttpServletRequest servletRequest = new MockHttpServletRequest("GET", "/api/v1/tenants");
        ServletServerHttpRequest request = new ServletServerHttpRequest(servletRequest);

        advice.beforeBodyWrite(response, null, null, null, request, null);

        assertThat(response.getPath()).isEqualTo("/api/v1/tenants");
        assertThat(response.getVersion()).isEqualTo(ApiConstants.API_VERSION);
    }
}
