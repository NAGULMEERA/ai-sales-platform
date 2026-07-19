package com.aisales.ai.infrastructure.llm;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Fail-fast in production when the Stub LLM is still selected.
 * AI recommends must use a real provider in prod (Rule 09).
 */
@Component
@Profile("prod")
public class ProdLlmGuard implements ApplicationRunner {

    private final String provider;

    public ProdLlmGuard(@Value("${aisales.ai.llm.provider:STUB}") String provider) {
        this.provider = provider;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (provider == null || "STUB".equalsIgnoreCase(provider.trim())) {
            throw new IllegalStateException(
                    "aisales.ai.llm.provider=STUB is forbidden when spring.profiles.active includes prod. "
                            + "Set aisales.ai.llm.provider=GEMINI or OPENAI "
                            + "(and enable the matching aisales.ai.llm.<provider>.enabled=true) before deploying.");
        }
    }
}
