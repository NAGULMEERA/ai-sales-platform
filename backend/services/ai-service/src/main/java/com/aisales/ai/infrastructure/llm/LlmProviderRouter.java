package com.aisales.ai.infrastructure.llm;

import com.aisales.ai.domain.llm.LlmClient;
import com.aisales.ai.domain.llm.LlmCompletionRequest;
import com.aisales.ai.domain.llm.LlmCompletionResult;
import com.aisales.ai.domain.llm.LlmProvider;
import com.aisales.ai.infrastructure.configuration.LlmProperties;
import com.aisales.common.exception.exception.BusinessException;
import com.aisales.common.exception.model.ErrorCode;
import java.util.List;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Selects the configured {@link LlmClient}. Gateway and business callers depend only on
 * {@link LlmProvider}; vendor SDKs stay behind clients.
 */
@Component
@RequiredArgsConstructor
public class LlmProviderRouter implements LlmProvider {

    private final List<LlmClient> clients;
    private final LlmProperties properties;

    @Override
    public String name() {
        return resolve().name();
    }

    @Override
    public LlmCompletionResult complete(LlmCompletionRequest request) {
        return resolve().complete(request);
    }

    LlmClient resolve() {
        String configured = properties.getProvider();
        if (!StringUtils.hasText(configured)) {
            throw new BusinessException(ErrorCode.AI_UNAVAILABLE, "aisales.ai.llm.provider is not set");
        }
        String key = configured.trim().toUpperCase(Locale.ROOT);
        return clients.stream()
                .filter(client -> key.equals(client.name().toUpperCase(Locale.ROOT)))
                .findFirst()
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.AI_UNAVAILABLE,
                        "No LLM client registered for provider=" + key
                                + ". Available: "
                                + clients.stream().map(LlmClient::name).sorted().toList()));
    }
}
