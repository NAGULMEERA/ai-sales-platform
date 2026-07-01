package com.aisales.common.exception.exception;

import com.aisales.common.exception.model.ErrorCode;

public class EventPublishException extends BusinessException {

    public EventPublishException(String message) {
        super(ErrorCode.EVENT_PUBLISH_ERROR, message);
    }

    public EventPublishException(String message, Throwable cause) {
        super(ErrorCode.EVENT_PUBLISH_ERROR, message, cause);
    }
}
