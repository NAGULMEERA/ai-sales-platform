package com.aisales.common.exception.handler;

import com.aisales.common.exception.exception.BusinessException;
import com.aisales.common.exception.exception.NotFoundException;
import com.aisales.common.exception.exception.UnauthorizedException;
import com.aisales.common.exception.exception.ValidationException;
import com.aisales.common.exception.model.ErrorCode;
import com.aisales.common.exception.model.ErrorResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import feign.Response;
import feign.codec.ErrorDecoder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

@Slf4j
@RequiredArgsConstructor
public class FeignErrorDecoder implements ErrorDecoder {

    private final ObjectMapper objectMapper;
    private final ErrorDecoder defaultDecoder = new Default();

    @Override
    public Exception decode(String methodKey, Response response) {
        if (response.body() == null) {
            return defaultDecoder.decode(methodKey, response);
        }
        try (InputStream bodyStream = response.body().asInputStream()) {
            String body = new String(bodyStream.readAllBytes(), StandardCharsets.UTF_8);
            ErrorResponse errorResponse = objectMapper.readValue(body, ErrorResponse.class);
            return mapToException(response.status(), errorResponse);
        } catch (IOException e) {
            log.warn("Failed to decode Feign error response for {}: {}", methodKey, e.getMessage());
            return defaultDecoder.decode(methodKey, response);
        }
    }

    private Exception mapToException(int status, ErrorResponse errorResponse) {
        String message = errorResponse.getMessage() != null
                ? errorResponse.getMessage()
                : "Remote service error";
        if (status == HttpStatus.NOT_FOUND.value()) {
            return new NotFoundException(message);
        }
        if (status == HttpStatus.UNAUTHORIZED.value()) {
            return new UnauthorizedException(message);
        }
        if (status == HttpStatus.BAD_REQUEST.value()) {
            return new ValidationException(message, errorResponse.getValidationErrors());
        }
        ErrorCode errorCode = ErrorCode.INTERNAL_ERROR;
        return new BusinessException(errorCode, message);
    }
}
