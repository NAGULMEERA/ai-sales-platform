package com.aisales.common.events.schema;

public class EventSchemaRegistryException extends RuntimeException {

    public EventSchemaRegistryException(String message) {
        super(message);
    }

    public EventSchemaRegistryException(String message, Throwable cause) {
        super(message, cause);
    }
}
