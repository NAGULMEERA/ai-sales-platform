package com.aisales.common.exception.exception;

import com.aisales.common.exception.model.ErrorCode;

public class TenantException extends BusinessException {

    public TenantException(String message) {
        super(ErrorCode.TENANT_ERROR, message);
    }

    public TenantException(String message, Throwable cause) {
        super(ErrorCode.TENANT_ERROR, message, cause);
    }
}
