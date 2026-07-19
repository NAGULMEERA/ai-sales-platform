package com.aisales.ai.infrastructure.llm;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.aisales.ai.domain.llm.LlmCompletionResult;
import com.aisales.ai.infrastructure.configuration.LlmProperties;
import com.aisales.common.exception.exception.BusinessException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

class GeminiLlmClientTest {

    private GeminiLlmClient client;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        LlmProperties properties = new LlmProperties();
        properties.getGemini().setApiKey("test-key");
        properties.getGemini().setModel("gemini-2.0-flash");
        objectMapper = new ObjectMapper();
        client = new GeminiLlmClient(properties, RestClient.builder(), objectMapper);
    }

    @Test
    void shouldMapJsonCompletionWithUsageAndConfidence() {
        ObjectNode response = objectMapper.createObjectNode();
        response.put("modelVersion", "gemini-2.0-flash");
        ArrayNode candidates = response.putArray("candidates");
        ObjectNode candidate = candidates.addObject();
        ArrayNode parts = candidate.putObject("content").putArray("parts");
        parts.addObject().put("text", "{\"recommendation\":\"QUALIFY\",\"confidence\":0.88}");
        ObjectNode usage = response.putObject("usageMetadata");
        usage.put("promptTokenCount", 33);
        usage.put("candidatesTokenCount", 11);

        LlmCompletionResult result = client.mapResponse(response, "fallback");

        assertThat(result.provider()).isEqualTo("GEMINI");
        assertThat(result.model()).isEqualTo("gemini-2.0-flash");
        assertThat(result.structuredOutput()).containsEntry("recommendation", "QUALIFY");
        assertThat(result.confidence()).isEqualTo(0.88);
        assertThat(result.promptTokens()).isEqualTo(33);
        assertThat(result.completionTokens()).isEqualTo(11);
    }

    @Test
    void shouldRejectEmptyCandidates() {
        ObjectNode response = objectMapper.createObjectNode();
        response.putArray("candidates");

        assertThatThrownBy(() -> client.mapResponse(response, "gemini-2.0-flash"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Invalid Gemini");
    }
}
