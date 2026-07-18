package com.aisales.integration.infrastructure.voice;

import com.aisales.common.exception.exception.BusinessException;
import com.aisales.common.exception.model.ErrorCode;
import com.aisales.integration.domain.voice.VoiceProvider;
import com.aisales.integration.infrastructure.configuration.VoiceProperties;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Routes outbound voice by {@code aisales.integration.voice.provider} ({@code STUB} | {@code TWILIO}).
 */
@Component
@RequiredArgsConstructor
public class VoiceProviderRegistry {

    private final List<VoiceProvider> providers;
    private final VoiceProperties properties;

    public VoiceProvider resolveDefault() {
        String configured = properties.getProvider();
        if (!StringUtils.hasText(configured)) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "aisales.integration.voice.provider is not set");
        }
        String key = configured.trim().toUpperCase(Locale.ROOT);
        return providers.stream()
                .filter(p -> key.equals(p.name().toUpperCase(Locale.ROOT)))
                .findFirst()
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.INTERNAL_ERROR,
                        "No voice provider registered for aisales.integration.voice.provider="
                                + key
                                + ". Available: "
                                + providers.stream()
                                        .map(VoiceProvider::name)
                                        .sorted()
                                        .collect(Collectors.joining(", "))
                                + ". For TWILIO set aisales.integration.voice.twilio.enabled=true."));
    }
}
