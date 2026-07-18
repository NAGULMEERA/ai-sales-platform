package com.aisales.ai.infrastructure.configuration;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "aisales.ai.llm")
public class LlmProperties {

    /**
     * Active provider key resolved by {@code LlmProviderRouter}.
     * Plug/flag switch: {@code STUB} | {@code OPENAI} | {@code GEMINI}.
     */
    private String provider = "STUB";

    private OpenAi openai = new OpenAi();

    private Gemini gemini = new Gemini();

    @Data
    public static class OpenAi {
        /** When false, OpenAI client bean is not registered. */
        private boolean enabled = false;
        private String apiKey = "";
        private String baseUrl = "https://api.openai.com/v1";
        private String model = "gpt-4o-mini";
        private Double temperature = 0.2;
        private Integer maxTokens = 2048;
        private int connectTimeoutMs = 3000;
        private int readTimeoutMs = 60000;
        /**
         * When true (default), requests JSON object responses when an expected-output hint is present.
         */
        private boolean jsonObjectResponse = true;
    }

    @Data
    public static class Gemini {
        /** When false, Gemini client bean is not registered. */
        private boolean enabled = false;
        private String apiKey = "";
        private String baseUrl = "https://generativelanguage.googleapis.com/v1beta";
        /** Model id without {@code models/} prefix. */
        private String model = "gemini-2.0-flash";
        private Double temperature = 0.2;
        private Integer maxOutputTokens = 2048;
        private int connectTimeoutMs = 3000;
        private int readTimeoutMs = 60000;
        /**
         * When true (default), requests {@code application/json} when an expected-output hint is present.
         */
        private boolean jsonObjectResponse = true;
    }
}
