package com.aisales.common.exception.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

/**
 * Platform-wide error codes. Domain-specific codes use {@code DOMAIN_NNN} pattern.
 */
@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // Generic platform
    INTERNAL_ERROR("ERR_001", "Internal server error", HttpStatus.INTERNAL_SERVER_ERROR),
    VALIDATION_ERROR("ERR_002", "Validation failed", HttpStatus.BAD_REQUEST),
    NOT_FOUND("ERR_003", "Resource not found", HttpStatus.NOT_FOUND),
    UNAUTHORIZED("ERR_004", "Unauthorized", HttpStatus.UNAUTHORIZED),
    FORBIDDEN("ERR_005", "Access forbidden", HttpStatus.FORBIDDEN),
    BUSINESS_ERROR("ERR_006", "Business rule violation", HttpStatus.UNPROCESSABLE_ENTITY),
    TENANT_ERROR("ERR_007", "Tenant operation failed", HttpStatus.BAD_REQUEST),
    EVENT_PUBLISH_ERROR("ERR_008", "Failed to publish event", HttpStatus.INTERNAL_SERVER_ERROR),
    CONFLICT("ERR_009", "Resource conflict", HttpStatus.CONFLICT),
    SERVICE_UNAVAILABLE("ERR_010", "Service unavailable", HttpStatus.SERVICE_UNAVAILABLE),

    // Authentication & authorization
    AUTH_INVALID_TOKEN("AUTH_001", "Invalid or expired token", HttpStatus.UNAUTHORIZED),
    AUTH_INSUFFICIENT_PERMISSIONS("AUTH_002", "Insufficient permissions", HttpStatus.FORBIDDEN),
    AUTH_OAUTH2_LOGIN_FAILED("AUTH_003", "OAuth2 login failed", HttpStatus.UNAUTHORIZED),
    AUTH_RATE_LIMIT("AUTH_004", "Too many authentication attempts", HttpStatus.TOO_MANY_REQUESTS),

    // Tenant
    TENANT_INACTIVE("TENANT_001", "Tenant is inactive or suspended", HttpStatus.FORBIDDEN),
    TENANT_ACCESS_DENIED("TENANT_002", "Cross-tenant access denied", HttpStatus.FORBIDDEN),

    // Lead domain
    LEAD_VALIDATION_FAILED("LEAD_001", "Invalid lead data", HttpStatus.BAD_REQUEST),
    LEAD_NOT_FOUND("LEAD_002", "Lead not found", HttpStatus.NOT_FOUND),

    // AI domain
    AI_RATE_LIMIT("AI_001", "AI rate limit exceeded", HttpStatus.TOO_MANY_REQUESTS),
    AI_UNAVAILABLE("AI_002", "AI service unavailable", HttpStatus.SERVICE_UNAVAILABLE),

    // Billing
    BILLING_PAYMENT_REQUIRED("BILLING_001", "Payment required to continue", HttpStatus.PAYMENT_REQUIRED);

    private final String code;
    private final String defaultMessage;
    private final HttpStatus httpStatus;
}
