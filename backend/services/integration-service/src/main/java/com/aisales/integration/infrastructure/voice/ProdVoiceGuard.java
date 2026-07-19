package com.aisales.integration.infrastructure.voice;

import com.aisales.integration.infrastructure.configuration.MetaLeadAdsProperties;
import com.aisales.integration.infrastructure.configuration.VoiceProperties;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Fail-fast in production when Meta Lead Ads is enabled but voice is still STUB
 * or Twilio credentials are incomplete.
 */
@Component
@Profile("prod")
public class ProdVoiceGuard implements ApplicationRunner {

    private final MetaLeadAdsProperties leadAdsProperties;
    private final VoiceProperties voiceProperties;

    public ProdVoiceGuard(MetaLeadAdsProperties leadAdsProperties, VoiceProperties voiceProperties) {
        this.leadAdsProperties = leadAdsProperties;
        this.voiceProperties = voiceProperties;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!leadAdsProperties.isEnabled()) {
            return;
        }
        String provider = voiceProperties.getProvider();
        if (provider == null || "STUB".equalsIgnoreCase(provider.trim())) {
            throw new IllegalStateException(
                    "aisales.integration.voice.provider=STUB is forbidden in prod when Meta Lead Ads is enabled. "
                            + "Set aisales.integration.voice.provider=TWILIO "
                            + "(and aisales.integration.voice.twilio.enabled=true) before deploying.");
        }
        if ("TWILIO".equalsIgnoreCase(provider.trim())) {
            VoiceProperties.Twilio twilio = voiceProperties.getTwilio();
            if (!twilio.isEnabled()) {
                throw new IllegalStateException(
                        "aisales.integration.voice.twilio.enabled=true is required in prod when provider=TWILIO");
            }
            if (!StringUtils.hasText(twilio.getAccountSid())
                    || !StringUtils.hasText(twilio.getAuthToken())
                    || !StringUtils.hasText(twilio.getFromNumber())) {
                throw new IllegalStateException(
                        "Twilio accountSid, authToken, and fromNumber are required in prod");
            }
        }
    }
}
