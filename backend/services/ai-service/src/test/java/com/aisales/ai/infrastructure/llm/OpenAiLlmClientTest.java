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

class OpenAiLlmClientTest {

    private OpenAiLlmClient client;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        LlmProperties properties = new LlmProperties();
        properties.getOpenai().setApiKey("test-key");
        properties.getOpenai().setModel("gpt-4o-mini");
        objectMapper = new ObjectMapper();
        client = new OpenAiLlmClient(properties, RestClient.builder(), objectMapper);
    }

    @Test
    void shouldMapJsonCompletionWithUsageAndConfidence() {
        ObjectNode response = objectMapper.createObjectNode();
        response.put("model", "gpt-4o-mini");
        ArrayNode choices = response.putArray("choices");
        ObjectNode choice = choices.addObject();
        choice.putObject("message")
                .put("content", "{\"recommendation\":\"QUALIFY\",\"confidence\":0.91}");
        ObjectNode usage = response.putObject("usage");
        usage.put("prompt_tokens", 40);
        usage.put("completion_tokens", 12);

        LlmCompletionResult result = client.mapResponse(response, "fallback");

        assertThat(result.provider()).isEqualTo("OPENAI");
        assertThat(result.model()).isEqualTo("gpt-4o-mini");
        assertThat(result.structuredOutput()).containsEntry("recommendation", "QUALIFY");
        assertThat(result.confidence()).isEqualTo(0.91);
        assertThat(result.promptTokens()).isEqualTo(40);
        assertThat(result.completionTokens()).isEqualTo(12);
    }

    @Test
    void shouldKeepPlainTextWithoutStructuredOutput() {
        ObjectNode response = objectMapper.createObjectNode();
        ArrayNode choices = response.putArray("choices");
        choices.addObject().putObject("message").put("content", "Just a sentence.");

        LlmCompletionResult result = client.mapResponse(response, "gpt-4o-mini");

        assertThat(result.rawText()).isEqualTo("Just a sentence.");
        assertThat(result.structuredOutput()).isEmpty();
        assertThat(result.confidence()).isNull();
    }

    @Test
    void shouldRejectEmptyChoices() {
        ObjectNode response = objectMapper.createObjectNode();
        response.putArray("choices");

        assertThatThrownBy(() -> client.mapResponse(response, "gpt-4o-mini"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Invalid OpenAI");
    }
}
