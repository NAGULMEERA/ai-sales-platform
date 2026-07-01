package com.aisales.common.exception.config;

import com.aisales.common.core.util.CorrelationIdUtils;
import com.aisales.common.core.util.TenantContext;
import com.aisales.common.exception.exception.BusinessException;
import com.aisales.common.exception.exception.ValidationException;
import com.aisales.common.exception.model.ErrorCode;
import com.aisales.common.exception.model.ErrorResponse;
import com.aisales.common.exception.model.ValidationError;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.slf4j.MDC;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusinessException(BusinessException ex, HttpServletRequest request) {
        ErrorCode errorCode = ex.getErrorCode();
        log.warn("Business exception [{}]: {}", errorCode.getCode(), ex.getMessage());
        ErrorResponse response = buildResponse(errorCode, ex.getMessage(), request);
        if (ex instanceof ValidationException validationException) {
            response.setValidationErrors(validationException.getValidationErrors());
        }
        return ResponseEntity.status(errorCode.getHttpStatus()).body(response);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(
            MethodArgumentNotValidException ex, HttpServletRequest request) {
        List<ValidationError> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(this::toValidationError)
                .collect(Collectors.toList());
        ErrorResponse response = buildResponse(ErrorCode.VALIDATION_ERROR, "Validation failed", request);
        response.setValidationErrors(errors);
        return ResponseEntity.badRequest().body(response);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleAuthenticationException(
            AuthenticationException ex, HttpServletRequest request) {
        ErrorResponse response = buildResponse(ErrorCode.AUTH_INVALID_TOKEN, ex.getMessage(), request);
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDeniedException(
            AccessDeniedException ex, HttpServletRequest request) {
        ErrorResponse response = buildResponse(ErrorCode.AUTH_INSUFFICIENT_PERMISSIONS, ex.getMessage(), request);
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMethodNotSupported(
            HttpRequestMethodNotSupportedException ex, HttpServletRequest request) {
        ErrorResponse response = buildResponse(ErrorCode.VALIDATION_ERROR, ex.getMessage(), request);
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex, HttpServletRequest request) {
        log.error("Unhandled exception", ex);
        ErrorResponse response = buildResponse(ErrorCode.INTERNAL_ERROR, "An unexpected error occurred", request);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

    private ErrorResponse buildResponse(ErrorCode errorCode, String message, HttpServletRequest request) {
        return ErrorResponse.of(
                errorCode,
                message,
                request.getRequestURI(),
                CorrelationIdUtils.getCorrelationId(),
                currentTraceId(),
                TenantContext.getTenantId(),
                TenantContext.getUserId());
    }

    private String currentTraceId() {
        String traceId = MDC.get("traceId");
        return traceId != null ? traceId : MDC.get("trace_id");
    }

    private ValidationError toValidationError(FieldError fieldError) {
        return ValidationError.builder()
                .field(fieldError.getField())
                .message(fieldError.getDefaultMessage())
                .rejectedValue(fieldError.getRejectedValue())
                .build();
    }
}
