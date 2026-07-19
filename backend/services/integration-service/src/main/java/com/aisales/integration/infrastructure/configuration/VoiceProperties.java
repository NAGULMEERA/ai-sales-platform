package com.aisales.integration.infrastructure.configuration;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Voice plug/flag: {@code aisales.integration.voice.provider} = {@code STUB} | {@code TWILIO}.
 */
@Data
@ConfigurationProperties(prefix = "aisales.integration.voice")
public class VoiceProperties {

    /** Active provider key. */
    private String provider = "STUB";

    private Stub stub = new Stub();

    private Twilio twilio = new Twilio();

    @Data
    public static class Stub {
        private boolean enabled = true;
        /**
         * When true, fills missing qualification attributes as if the agent captured them.
         */
        private boolean simulateAnswers = true;
    }

    @Data
    public static class Twilio {
        private boolean enabled = false;
        private String accountSid = "";
        private String authToken = "";
        private String fromNumber = "";
        private String baseUrl = "https://api.twilio.com";
        /**
         * Optional hosted TwiML URL. When blank, inline TwiML {@code Say} script is used.
         */
        private String twimlUrl = "";
        /** Public HTTPS callback for call status updates (optional). */
        private String statusCallbackUrl = "";
        private int connectTimeoutMs = 3000;
        private int readTimeoutMs = 15000;
    }
}
