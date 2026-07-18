package com.aisales.common.events.schema;

import com.aisales.common.events.model.BaseEvent;
import com.aisales.common.events.model.CustomerCreatedEvent;
import com.aisales.common.events.model.EmailVerificationRequestedEvent;
import com.aisales.common.events.model.EmailVerifiedEvent;
import com.aisales.common.events.model.LeadAssignedEvent;
import com.aisales.common.events.model.LeadContactedEvent;
import com.aisales.common.events.model.LeadConvertedEvent;
import com.aisales.common.events.model.LeadCreatedEvent;
import com.aisales.common.events.model.LeadLostEvent;
import com.aisales.common.events.model.LeadQualifiedEvent;
import com.aisales.common.events.model.LeadScoredEvent;
import com.aisales.common.events.model.LeadStatusChangedEvent;
import com.aisales.common.events.model.LeadValidatedEvent;
import com.aisales.common.events.model.NotificationSentEvent;
import com.aisales.common.events.model.PasswordResetRequestedEvent;
import com.aisales.common.events.model.WorkflowCompletedEvent;
import com.aisales.common.events.model.TenantActivatedEvent;
import com.aisales.common.events.model.TenantCreatedEvent;
import com.aisales.common.events.model.TenantDeletedEvent;
import com.aisales.common.events.model.TenantSuspendedEvent;
import com.aisales.common.events.model.TenantUpdatedEvent;
import com.aisales.common.events.model.UserCreatedEvent;
import com.aisales.common.events.model.UserDeletedEvent;
import com.aisales.common.events.model.UserUpdatedEvent;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Set;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class EventSchemaRegistryTest {

    private final JsonMapper objectMapper = JsonMapper.builder()
            .findAndAddModules()
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .build();
    private final EventSchemaRegistry schemaRegistry = new EventSchemaRegistry(objectMapper);

    @ParameterizedTest
    @MethodSource("eventSamples")
    void shouldRegisterSchemaForSerializedEvent(BaseEvent event) {
        JsonNode schema = schemaRegistry.getSchema(event.getEventType(), event.getEventVersion());
        JsonNode payload = objectMapper.valueToTree(event);

        validate(schema, payload);
    }

    @Test
    void shouldCoverEveryConcreteEventClassWithSchemaTestSample() throws Exception {
        Set<String> sampledEvents = eventSamples()
                .map(event -> event.getClass().getSimpleName())
                .collect(java.util.stream.Collectors.toSet());

        try (Stream<Path> eventClasses = Files.list(Path.of("src/main/java/com/aisales/common/events/model"))) {
            Set<String> concreteEventClasses = eventClasses
                    .map(path -> path.getFileName().toString())
                    .filter(name -> name.endsWith("Event.java"))
                    .filter(name -> !"BaseEvent.java".equals(name))
                    .map(name -> name.substring(0, name.length() - ".java".length()))
                    .collect(java.util.stream.Collectors.toSet());

            assertThat(sampledEvents).containsExactlyInAnyOrderElementsOf(concreteEventClasses);
        }
    }

    private static Stream<BaseEvent> eventSamples() {
        return Stream.of(
                TenantCreatedEvent.of("tenant-1", "Acme", "acme", "FREE", "REAL_ESTATE", "corr-1"),
                TenantUpdatedEvent.of("tenant-1", "Acme Updated", "ACTIVE", "corr-1"),
                TenantActivatedEvent.of("tenant-1", "Acme", "acme", "corr-1"),
                TenantSuspendedEvent.of("tenant-1", "Acme", "acme", "corr-1"),
                TenantDeletedEvent.of("tenant-1", "corr-1"),
                UserCreatedEvent.of("tenant-1", "user-1", "user@example.com", "Ada", "Lovelace",
                        Set.of("USER"), "corr-1"),
                UserUpdatedEvent.of("tenant-1", "user-1", "user@example.com", "ACTIVE", "corr-1"),
                UserDeletedEvent.of("tenant-1", "user-1", "corr-1"),
                LeadCreatedEvent.of("tenant-1", "lead-1", "Jane Lead", "WEB", "NEW", "corr-1"),
                LeadValidatedEvent.of("tenant-1", "lead-1", "Jane Lead", "corr-1"),
                LeadQualifiedEvent.of("tenant-1", "lead-1", "Jane Lead", 80, "QUALIFIED", "corr-1"),
                LeadAssignedEvent.of("tenant-1", "lead-1", "Jane Lead", "user-2", "round-robin", "corr-1"),
                LeadContactedEvent.of("tenant-1", "lead-1", "Jane Lead", "WHATSAPP", "corr-1"),
                LeadScoredEvent.of("tenant-1", "lead-1", "Jane Lead", 80, "AI", "corr-1"),
                LeadConvertedEvent.of("tenant-1", "lead-1", "Jane Lead", "customer-1", "corr-1"),
                LeadLostEvent.of("tenant-1", "lead-1", "Jane Lead", "no budget", "corr-1"),
                LeadStatusChangedEvent.of("tenant-1", "lead-1", "Jane Lead",
                        "NEW", "CONTACTED", "first touch", "corr-1"),
                CustomerCreatedEvent.of("tenant-1", "customer-1", "Jane Customer",
                        "customer@example.com", "corr-1"),
                NotificationSentEvent.of("tenant-1", "notification-1", "EMAIL",
                        "customer@example.com", "welcome", "corr-1"),
                EmailVerificationRequestedEvent.of("tenant-1", "user-1", "user@example.com", "Ada",
                        "token-1", "https://app.example/verify?token=token-1", "corr-1"),
                PasswordResetRequestedEvent.of("tenant-1", "user-1", "user@example.com", "Ada",
                        "token-2", "https://app.example/reset?token=token-2", "corr-1"),
                EmailVerifiedEvent.of("tenant-1", "user-1", "user@example.com", "corr-1"),
                WorkflowCompletedEvent.of("tenant-1", "wf-1", "ONBOARDING_V1", "user-1", "corr-1")
        );
    }

    private static void validate(JsonNode schema, JsonNode payload) {
        for (JsonNode requiredField : schema.path("required")) {
            String field = requiredField.asText();
            assertThat(payload.hasNonNull(field))
                    .as("required field %s should be present", field)
                    .isTrue();
        }

        JsonNode properties = schema.path("properties");
        properties.fields().forEachRemaining(entry -> validateProperty(entry.getKey(), entry.getValue(), payload));
    }

    private static void validateProperty(String field, JsonNode propertySchema, JsonNode payload) {
        JsonNode value = payload.get(field);
        if (value == null || value.isNull()) {
            return;
        }

        if (propertySchema.has("const")) {
            assertThat(value).as("const field %s", field).isEqualTo(propertySchema.get("const"));
        }
        if (propertySchema.has("type")) {
            assertType(field, value, propertySchema.get("type").asText());
        }
        if (propertySchema.path("minLength").asInt(0) > 0) {
            assertThat(value.asText()).as("minLength field %s", field).isNotBlank();
        }
        if ("date-time".equals(propertySchema.path("format").asText())) {
            assertThat(Instant.parse(value.asText())).as("date-time field %s", field).isNotNull();
        }
    }

    private static void assertType(String field, JsonNode value, String type) {
        switch (type) {
            case "string" -> assertThat(value.isTextual()).as("string field %s", field).isTrue();
            case "integer" -> assertThat(value.isIntegralNumber()).as("integer field %s", field).isTrue();
            case "array" -> assertThat(value.isArray()).as("array field %s", field).isTrue();
            case "object" -> assertThat(value.isObject()).as("object field %s", field).isTrue();
            default -> throw new IllegalArgumentException("Unsupported schema type in test: " + type);
        }
    }
}
