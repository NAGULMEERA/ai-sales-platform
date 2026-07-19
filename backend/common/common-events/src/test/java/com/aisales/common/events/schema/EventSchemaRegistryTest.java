package com.aisales.common.events.schema;

import com.aisales.common.events.model.AiQualificationCompletedEvent;
import com.aisales.common.events.model.AiRecommendationGeneratedEvent;
import com.aisales.common.events.model.BaseEvent;
import com.aisales.common.events.model.CatalogMatchedEvent;
import com.aisales.common.events.model.CatalogOfferCreatedEvent;
import com.aisales.common.events.model.CatalogProductCreatedEvent;
import com.aisales.common.events.model.CatalogProductUpdatedEvent;
import com.aisales.common.events.model.CatalogRecommendationGeneratedEvent;
import com.aisales.common.events.model.AiReplyGeneratedEvent;
import com.aisales.common.events.model.ConversationClosedEvent;
import com.aisales.common.events.model.ConversationMessageAddedEvent;
import com.aisales.common.events.model.ConversationStartedEvent;
import com.aisales.common.events.model.ConversationSummarizedEvent;
import com.aisales.common.events.model.MessageReceivedEvent;
import com.aisales.common.events.model.MessageSentEvent;
import com.aisales.common.events.model.WorkflowTriggeredEvent;
import com.aisales.common.events.model.CustomerArchivedEvent;
import com.aisales.common.events.model.CustomerCreatedEvent;
import com.aisales.common.events.model.CustomerDeactivatedEvent;
import com.aisales.common.events.model.CustomerMergedEvent;
import com.aisales.common.events.model.CustomerUpdatedEvent;
import com.aisales.common.events.model.CustomerVerifiedEvent;
import com.aisales.common.events.model.LeadConvertedToCustomerEvent;
import com.aisales.common.events.model.LeadMergedEvent;
import com.aisales.common.events.model.LeadReopenedEvent;
import com.aisales.common.events.model.LeadUnassignedEvent;
import com.aisales.common.events.model.SubscriptionPlanChangedEvent;
import com.aisales.common.events.model.EmailVerificationRequestedEvent;
import com.aisales.common.events.model.EmailVerifiedEvent;
import com.aisales.common.events.model.LeadArchivedEvent;
import com.aisales.common.events.model.LeadAssignedEvent;
import com.aisales.common.events.model.LeadContactedEvent;
import com.aisales.common.events.model.LeadConvertedEvent;
import com.aisales.common.events.model.LeadCreatedEvent;
import com.aisales.common.events.model.LeadLostEvent;
import com.aisales.common.events.model.LeadQualifiedEvent;
import com.aisales.common.events.model.LeadScoredEvent;
import com.aisales.common.events.model.LeadStatusChangedEvent;
import com.aisales.common.events.model.LeadValidatedEvent;
import com.aisales.common.events.model.LeadVisitCancelledEvent;
import com.aisales.common.events.model.LeadVisitCompletedEvent;
import com.aisales.common.events.model.KnowledgeBaseCreatedEvent;
import com.aisales.common.events.model.KnowledgeDocumentRegisteredEvent;
import com.aisales.common.events.model.KnowledgeRetrievedEvent;
import com.aisales.common.events.model.LeadVisitScheduledEvent;
import com.aisales.common.events.model.NotificationSentEvent;
import com.aisales.common.events.model.OpportunityAssignedEvent;
import com.aisales.common.events.model.OpportunityLostEvent;
import com.aisales.common.events.model.OpportunityWonEvent;
import com.aisales.common.events.model.PromptExecutedEvent;
import com.aisales.common.events.model.OpportunityCreatedEvent;
import com.aisales.common.events.model.SemanticCacheHitEvent;
import com.aisales.common.events.model.SemanticCacheMissEvent;
import com.aisales.common.events.model.OpportunityStatusChangedEvent;
import com.aisales.common.events.model.PasswordResetRequestedEvent;
import com.aisales.common.events.model.PluginDisabledEvent;
import com.aisales.common.events.model.PluginEnabledEvent;
import com.aisales.common.events.model.QuoteAcceptedEvent;
import com.aisales.common.events.model.QuoteCreatedEvent;
import com.aisales.common.events.model.QuoteSentEvent;
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
                LeadVisitScheduledEvent.of("tenant-1", "lead-1", "Jane Lead",
                        Instant.parse("2026-07-20T10:00:00Z"), "Site A", "corr-1"),
                LeadVisitCompletedEvent.of("tenant-1", "lead-1", "Jane Lead", "liked the view", "corr-1"),
                LeadVisitCancelledEvent.of("tenant-1", "lead-1", "Jane Lead", "reschedule", "corr-1"),
                LeadArchivedEvent.of("tenant-1", "lead-1", "Jane Lead", "retention policy", "corr-1"),
                LeadMergedEvent.of("tenant-1", "lead-1", "Jane Lead", "lead-2", "corr-1"),
                LeadReopenedEvent.of("tenant-1", "lead-1", "Jane Lead", "LOST", "QUALIFIED", "new interest", "corr-1"),
                LeadUnassignedEvent.of("tenant-1", "lead-1", "Jane Lead", "user-2", "released", "corr-1"),
                CustomerCreatedEvent.of("tenant-1", "customer-1", "Jane Customer",
                        "customer@example.com", "corr-1"),
                CustomerUpdatedEvent.of("tenant-1", "customer-1", "Jane Customer", "ACTIVE", "corr-1"),
                CustomerArchivedEvent.of("tenant-1", "customer-1", "Jane Customer", "churn", "corr-1"),
                CustomerMergedEvent.of("tenant-1", "customer-1", "Jane Customer", "customer-2", "corr-1"),
                CustomerVerifiedEvent.of("tenant-1", "customer-1", "Jane Customer", "EMAIL",
                        "customer@example.com", "corr-1"),
                CustomerDeactivatedEvent.of("tenant-1", "customer-1", "Jane Customer", "pause", "corr-1"),
                LeadConvertedToCustomerEvent.of("tenant-1", "customer-1", "Jane Customer",
                        "lead-1", false, "corr-1"),
                SubscriptionPlanChangedEvent.of("tenant-1", "FREE", "PREMIUM", "sub-1", "corr-1"),
                CatalogProductCreatedEvent.of("tenant-1", "product-1", "SKU-1", "Studio Plan",
                        "PRODUCT", "ACTIVE", "corr-1"),
                CatalogProductUpdatedEvent.of("tenant-1", "product-1", "SKU-1", "Studio Plan",
                        "PRODUCT", "ACTIVE", "corr-1"),
                CatalogOfferCreatedEvent.of("tenant-1", "offer-1", "product-1", "OFFER-1",
                        "INR", "2500000.0000", "ACTIVE", "corr-1"),
                CatalogMatchedEvent.of("tenant-1", "lead-1", "3", "corr-1"),
                CatalogRecommendationGeneratedEvent.of(
                        "tenant-1", "lead-1", "lead-1", "2", "product-1", "0.88", "corr-1"),
                ConversationStartedEvent.of("tenant-1", "conv-1", "lead-1", "customer-1",
                        "WEB", "OPEN", "corr-1"),
                ConversationMessageAddedEvent.of("tenant-1", "conv-1", "lead-1", "msg-1",
                        "CUSTOMER", "corr-1"),
                MessageReceivedEvent.of("tenant-1", "conv-1", "lead-1", "msg-1",
                        "CUSTOMER", "WEB", "corr-key", "corr-1"),
                MessageSentEvent.of("tenant-1", "conv-1", "lead-1", "msg-2",
                        "AGENT", "WHATSAPP", "corr-key-2", "corr-1"),
                AiReplyGeneratedEvent.of("tenant-1", "conv-1", "lead-1", "exec-1",
                        "BUY_INTENT", "0.9", "FOLLOW_UP", "corr-1"),
                ConversationSummarizedEvent.of("tenant-1", "conv-1", "lead-1", "exec-1",
                        "POSITIVE", "BUY_INTENT", "corr-1"),
                ConversationClosedEvent.of("tenant-1", "conv-1", "lead-1", "resolved", "corr-1"),
                OpportunityCreatedEvent.of("tenant-1", "opp-1", "customer-1", "lead-1",
                        "Studio deal", "OPEN", "user-2", "corr-1"),
                OpportunityAssignedEvent.of("tenant-1", "opp-1", "customer-1",
                        "user-3", "user-2", "corr-1"),
                OpportunityStatusChangedEvent.of("tenant-1", "opp-1", "customer-1",
                        "OPEN", "QUOTED", "quote sent", "corr-1"),
                OpportunityWonEvent.of("tenant-1", "opp-1", "customer-1", "lead-1", "closed", "corr-1"),
                OpportunityLostEvent.of("tenant-1", "opp-1", "customer-1", "lead-1", "budget", "corr-1"),
                QuoteCreatedEvent.of("tenant-1", "quote-1", "opp-1", "DRAFT",
                        "INR", "2500000.00", "1", "corr-1"),
                QuoteSentEvent.of("tenant-1", "quote-1", "opp-1", "SENT",
                        "2500000.00", "corr-1"),
                QuoteAcceptedEvent.of("tenant-1", "quote-1", "opp-1", "ACCEPTED",
                        "2500000.00", "corr-1"),
                PromptExecutedEvent.of("tenant-1", "exec-1", "LEAD_QUALIFY_V1", "1",
                        "STUB", "stub-model", "0.85", "lead-1", "corr-1"),
                AiQualificationCompletedEvent.of(
                        "tenant-1",
                        "exec-1",
                        "LEAD_QUALIFY_REAL_ESTATE",
                        "1",
                        "STUB",
                        "stub-model",
                        "QUALIFY",
                        "82",
                        "90",
                        "lead-1",
                        false,
                        "corr-1"),
                AiRecommendationGeneratedEvent.of(
                        "tenant-1",
                        "exec-1",
                        "LEAD_QUALIFICATION",
                        "Schedule site visit",
                        "0.9",
                        "lead-1",
                        "corr-1"),
                KnowledgeRetrievedEvent.of(
                        "tenant-1", "kb-1", "HYBRID", "3", "query-hash", "corr-1"),
                SemanticCacheHitEvent.of("tenant-1", "LEAD_QUALIFY|v1", "stub-model", "corr-1"),
                SemanticCacheMissEvent.of("tenant-1", "LEAD_QUALIFY|v1", "stub-model", "corr-1"),
                KnowledgeBaseCreatedEvent.of("tenant-1", "kb-1", "DEFAULT",
                        "Default KB", "ACTIVE", "corr-1"),
                KnowledgeDocumentRegisteredEvent.of("tenant-1", "doc-1", "kb-1",
                        "FAQ.pdf", "PENDING", "media-1", "corr-1"),
                PluginEnabledEvent.of("tenant-1", "install-1", "email-channel",
                        "CAPABILITY", "1.0.0", "corr-1"),
                PluginDisabledEvent.of("tenant-1", "install-1", "email-channel",
                        "CAPABILITY", "1.0.0", "corr-1"),
                NotificationSentEvent.of("tenant-1", "notification-1", "EMAIL",
                        "customer@example.com", "welcome", "corr-1"),
                EmailVerificationRequestedEvent.of("tenant-1", "user-1", "user@example.com", "Ada",
                        "token-1", "https://app.example/verify?token=token-1", "corr-1"),
                PasswordResetRequestedEvent.of("tenant-1", "user-1", "user@example.com", "Ada",
                        "token-2", "https://app.example/reset?token=token-2", "corr-1"),
                EmailVerifiedEvent.of("tenant-1", "user-1", "user@example.com", "corr-1"),
                WorkflowCompletedEvent.of("tenant-1", "wf-1", "ONBOARDING_V1", "user-1", "corr-1"),
                WorkflowTriggeredEvent.of(
                        "tenant-1",
                        "wf-2",
                        "AUTOMATION_RULE_V1",
                        "FOLLOWUP_ON_MESSAGE",
                        "MESSAGE_RECEIVED",
                        "conv-1",
                        "corr-1")
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
            case "boolean" -> assertThat(value.isBoolean()).as("boolean field %s", field).isTrue();
            case "array" -> assertThat(value.isArray()).as("array field %s", field).isTrue();
            case "object" -> assertThat(value.isObject()).as("object field %s", field).isTrue();
            default -> throw new IllegalArgumentException("Unsupported schema type in test: " + type);
        }
    }
}
