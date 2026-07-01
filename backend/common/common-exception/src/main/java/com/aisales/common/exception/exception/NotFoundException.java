package com.aisales.common.exception.exception;

import com.aisales.common.exception.model.ErrorCode;

public class NotFoundException extends BusinessException {

    public NotFoundException(String message) {
        super(ErrorCode.NOT_FOUND, message);
    }

    public NotFoundException(String resource, Object id) {
        super(ErrorCode.NOT_FOUND, String.format("%s not found with id: %s", resource, id));
    }
}
