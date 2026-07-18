package com.aisales.integration.infrastructure.voice;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.aisales.common.exception.exception.BusinessException;
import com.aisales.integration.domain.voice.VoiceProvider;
import com.aisales.integration.infrastructure.configuration.VoiceProperties;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class VoiceProviderRegistryTest {

    @Test
    void shouldResolveConfiguredProvider() {
        VoiceProperties properties = new VoiceProperties();
        properties.setProvider("STUB");
        StubVoiceProvider stub = new StubVoiceProvider(properties);
        VoiceProviderRegistry registry = new VoiceProviderRegistry(List.of(stub), properties);

        assertThat(registry.resolveDefault().name()).isEqualTo("STUB");
    }

    @Test
    void shouldFailWhenProviderMissing() {
        VoiceProperties properties = new VoiceProperties();
        properties.setProvider("TWILIO");
        StubVoiceProvider stub = new StubVoiceProvider(properties);
        VoiceProviderRegistry registry = new VoiceProviderRegistry(List.of(stub), properties);

        assertThatThrownBy(registry::resolveDefault)
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("TWILIO");
    }

    @Test
    void stubShouldSimulateAnswers() {
        VoiceProperties properties = new VoiceProperties();
        properties.getStub().setSimulateAnswers(true);
        StubVoiceProvider stub = new StubVoiceProvider(properties);

        VoiceProvider.VoiceCallResult result = stub.placeOutboundCall(new VoiceProvider.VoiceCallRequest(
                UUID.randomUUID(), UUID.randomUUID(), "+919999999999", "Ada", Map.of()));

        assertThat(result.status()).isEqualTo("COMPLETED");
        assertThat(result.capturedAttributes()).containsKeys("budget", "location", "timeline");
    }
}
