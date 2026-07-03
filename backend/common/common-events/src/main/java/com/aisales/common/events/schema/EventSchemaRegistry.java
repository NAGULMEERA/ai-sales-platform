package com.aisales.common.events.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;

import java.io.IOException;
import java.io.InputStream;

@RequiredArgsConstructor
public class EventSchemaRegistry {

    private static final String SCHEMA_PATH_TEMPLATE = "schemas/events/%s.v%d.json";

    private final ObjectMapper objectMapper;

    public JsonNode getSchema(String eventType, int eventVersion) {
        String path = schemaPath(eventType, eventVersion);
        try (InputStream inputStream = Thread.currentThread().getContextClassLoader().getResourceAsStream(path)) {
            if (inputStream == null) {
                throw new EventSchemaRegistryException(
                        "No schema registered for event " + eventType + " v" + eventVersion);
            }
            return objectMapper.readTree(inputStream);
        } catch (IOException ex) {
            throw new EventSchemaRegistryException(
                    "Failed to load schema for event " + eventType + " v" + eventVersion, ex);
        }
    }

    public boolean hasSchema(String eventType, int eventVersion) {
        String path = schemaPath(eventType, eventVersion);
        return Thread.currentThread().getContextClassLoader().getResource(path) != null;
    }

    private static String schemaPath(String eventType, int eventVersion) {
        return SCHEMA_PATH_TEMPLATE.formatted(eventType, eventVersion);
    }
}
