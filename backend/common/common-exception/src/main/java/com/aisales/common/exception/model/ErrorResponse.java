package com.aisales.common.exception.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {

    private Instant timestamp;
    private String correlationId;
    private String traceId;
    private int status;
    private ErrorDetail error;
    private String path;
    private String tenantId;
    private String userId;

    /** @deprecated Use {@link #error} nested object; retained for backward compatibility. */
    @Deprecated
    private String code;

    /** @deprecated Use {@link #error} nested object; retained for backward compatibility. */
    @Deprecated
    private String message;

    private List<ValidationError> validationErrors;
    private List<ApiError> errors;

    public static ErrorResponse of(ErrorCode errorCode, String message, String path,
                                   String correlationId, String traceId,
                                   String tenantId, String userId) {
        String resolvedMessage = message != null ? message : errorCode.getDefaultMessage();
        ErrorDetail detail = ErrorDetail.builder()
                .code(errorCode.getCode())
                .message(resolvedMessage)
                .build();
        return ErrorResponse.builder()
                .timestamp(Instant.now())
                .correlationId(correlationId)
                .traceId(traceId)
                .status(errorCode.getHttpStatus().value())
                .error(detail)
                .code(errorCode.getCode())
                .message(resolvedMessage)
                .path(path)
                .tenantId(tenantId)
                .userId(userId)
                .build();
    }

    public static ErrorResponse of(ErrorCode errorCode, String message, String path, String correlationId) {
        return of(errorCode, message, path, correlationId, null, null, null);
    }
}
