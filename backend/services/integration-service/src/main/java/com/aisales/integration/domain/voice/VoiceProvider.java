package com.aisales.integration.domain.voice;

import java.util.Map;
import java.util.UUID;

/**
 * Pluggable outbound voice backend. Selected by {@code aisales.integration.voice.provider}.
 */
public interface VoiceProvider {

    String name();

    VoiceCallResult placeOutboundCall(VoiceCallRequest request);

    record VoiceCallRequest(
            UUID leadId,
            UUID tenantId,
            String toPhone,
            String customerName,
            Map<String, Object> knownAttributes) {
    }

    record VoiceCallResult(
            String providerCallId,
            String status,
            Map<String, Object> capturedAttributes,
            String failureMessage) {

        public static VoiceCallResult completed(
                String providerCallId, Map<String, Object> capturedAttributes) {
            return new VoiceCallResult(providerCallId, "COMPLETED", capturedAttributes, null);
        }

        public static VoiceCallResult queued(String providerCallId) {
            return new VoiceCallResult(providerCallId, "QUEUED", Map.of(), null);
        }

        public static VoiceCallResult failed(String providerCallId, String message) {
            return new VoiceCallResult(providerCallId, "FAILED", Map.of(), message);
        }

        public boolean succeeded() {
            return "COMPLETED".equalsIgnoreCase(status) || "QUEUED".equalsIgnoreCase(status);
        }
    }
}
