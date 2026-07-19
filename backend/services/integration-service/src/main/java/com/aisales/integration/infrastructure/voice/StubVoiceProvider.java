package com.aisales.integration.infrastructure.voice;

import com.aisales.integration.domain.voice.VoiceProvider;
import com.aisales.integration.infrastructure.configuration.VoiceProperties;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Local/dev voice: no PSTN. Optionally simulates captured qualification answers.
 */
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "aisales.integration.voice.stub.enabled", havingValue = "true", matchIfMissing = true)
public class StubVoiceProvider implements VoiceProvider {

    public static final String NAME = "STUB";

    private final VoiceProperties properties;

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public VoiceCallResult placeOutboundCall(VoiceCallRequest request) {
        Map<String, Object> captured = new HashMap<>();
        if (properties.getStub().isSimulateAnswers()) {
            captured.put("budget", "50-80L");
            captured.put("location", "Bengaluru");
            captured.put("timeline", "3-6 months");
        }
        String callId = "stub_" + (request.leadId() != null ? request.leadId() : UUID.randomUUID());
        return VoiceCallResult.completed(callId, captured);
    }
}
