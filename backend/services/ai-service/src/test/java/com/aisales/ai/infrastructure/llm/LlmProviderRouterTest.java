package com.aisales.ai.infrastructure.llm;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.aisales.ai.domain.llm.LlmClient;
import com.aisales.ai.domain.llm.LlmCompletionRequest;
import com.aisales.ai.domain.llm.LlmCompletionResult;
import com.aisales.ai.infrastructure.configuration.LlmProperties;
import com.aisales.common.exception.exception.BusinessException;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class LlmProviderRouterTest {

    @Test
    void shouldRouteToConfiguredClient() {
        LlmProperties properties = new LlmProperties();
        properties.setProvider("STUB");
        LlmProviderRouter router = new LlmProviderRouter(List.of(new StubLlmProvider()), properties);

        LlmCompletionResult result = router.complete(new LlmCompletionRequest(
                "sys", "user hello", "TEST", null, Map.of()));

        assertThat(router.name()).isEqualTo("STUB");
        assertThat(result.provider()).isEqualTo("STUB");
        assertThat(result.structuredOutput()).containsEntry("recommendation", "REVIEW");
    }

    @Test
    void shouldFailWhenProviderMissing() {
        LlmProperties properties = new LlmProperties();
        properties.setProvider("GEMINI");
        LlmProviderRouter router = new LlmProviderRouter(List.of(new StubLlmProvider()), properties);

        assertThatThrownBy(() -> router.resolve())
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("GEMINI");
    }

    @Test
    void shouldMatchProviderCaseInsensitively() {
        LlmProperties properties = new LlmProperties();
        properties.setProvider("openai");
        LlmClient openAi = new LlmClient() {
            @Override
            public String name() {
                return "OPENAI";
            }

            @Override
            public LlmCompletionResult complete(LlmCompletionRequest request) {
                return new LlmCompletionResult("OPENAI", "gpt", "{}", Map.of(), 0.5, 1, 1);
            }
        };
        LlmProviderRouter router = new LlmProviderRouter(List.of(new StubLlmProvider(), openAi), properties);
        assertThat(router.resolve().name()).isEqualTo("OPENAI");
    }

    @Test
    void shouldRouteToGeminiWhenConfigured() {
        LlmProperties properties = new LlmProperties();
        properties.setProvider("GEMINI");
        LlmClient gemini = new LlmClient() {
            @Override
            public String name() {
                return "GEMINI";
            }

            @Override
            public LlmCompletionResult complete(LlmCompletionRequest request) {
                return new LlmCompletionResult("GEMINI", "gemini-2.0-flash", "{}", Map.of(), 0.9, 2, 2);
            }
        };
        LlmProviderRouter router = new LlmProviderRouter(List.of(new StubLlmProvider(), gemini), properties);
        assertThat(router.resolve().name()).isEqualTo("GEMINI");
        assertThat(router.complete(new LlmCompletionRequest("s", "u", "p", null, Map.of())).provider())
                .isEqualTo("GEMINI");
    }
}
