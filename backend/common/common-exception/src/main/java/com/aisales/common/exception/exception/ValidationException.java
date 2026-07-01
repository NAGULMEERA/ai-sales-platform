package com.aisales.common.exception.exception;

import com.aisales.common.exception.model.ErrorCode;
import com.aisales.common.exception.model.ValidationError;
import lombok.Getter;

import java.util.Collections;
import java.util.List;

@Getter
public class ValidationException extends BusinessException {

    private final List<ValidationError> validationErrors;

    public ValidationException(String message) {
        super(ErrorCode.VALIDATION_ERROR, message);
        this.validationErrors = Collections.emptyList();
    }

    public ValidationException(String message, List<ValidationError> validationErrors) {
        super(ErrorCode.VALIDATION_ERROR, message);
        this.validationErrors = validationErrors != null ? validationErrors : Collections.emptyList();
    }
}
