package com.aisales.common.events.consumer;

public class IntegrationEventProcessingException extends RuntimeException {

    public IntegrationEventProcessingException(String message, Throwable cause) {
        super(message, cause);
    }
}
