package com.aisales.common.core.web;

import com.aisales.common.core.constant.ApiConstants;
import com.aisales.common.core.dto.ApiResponse;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

@ControllerAdvice
public class ApiResponseEnrichmentAdvice implements ResponseBodyAdvice<Object> {

    @Override
    public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
        return ApiResponse.class.isAssignableFrom(returnType.getParameterType());
    }

    @Override
    public Object beforeBodyWrite(Object body, MethodParameter returnType, MediaType selectedContentType,
                                  Class<? extends HttpMessageConverter<?>> selectedConverterType,
                                  ServerHttpRequest request, ServerHttpResponse response) {
        if (body instanceof ApiResponse<?> apiResponse && request instanceof ServletServerHttpRequest servletRequest) {
            if (apiResponse.getPath() == null) {
                apiResponse.setPath(servletRequest.getServletRequest().getRequestURI());
            }
            if (apiResponse.getVersion() == null) {
                apiResponse.setVersion(ApiConstants.API_VERSION);
            }
        }
        return body;
    }
}
