package com.aisales.ai.application.service;

import com.aisales.ai.domain.llm.LlmCompletionRequest;
import com.aisales.ai.domain.llm.LlmCompletionResult;
import com.aisales.ai.domain.llm.LlmProvider;
import com.aisales.common.contracts.ai.AiExecuteRequest;
import com.aisales.common.contracts.ai.AiExecuteResponse;
import com.aisales.common.core.util.CorrelationIdUtils;
import com.aisales.common.core.util.TenantContext;
import com.aisales.common.events.model.PromptExecutedEvent;
import com.aisales.common.events.publisher.EventPublisher;
import com.aisales.common.exception.exception.ValidationException;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * AI Gateway entry point. Resolves versioned prompts, renders variables,
 * invokes a provider-abstracted LLM, and returns structured output for business validation.
 */
@Service
@RequiredArgsConstructor
public class AiGatewayService {

    private final PromptService promptService;
    private final PromptRenderer promptRenderer;
    private final LlmProvider llmProvider;
    private final EventPublisher eventPublisher;

    @Transactional
    public AiExecuteResponse execute(AiExecuteRequest request) {
        UUID tenantId = requireTenantId();
        PromptService.ResolvedPrompt resolved = promptService.resolveForExecution(
                request.getPromptCode(), request.getPromptId(), request.getPromptVersion());

        // System prompt may omit declared variables; user prompt enforces them.
        String system = promptRenderer.render(
                resolved.version().getSystemTemplate(),
                request.getVariables(),
                List.of());
        String user = promptRenderer.render(
                resolved.version().getUserTemplate(),
                request.getVariables(),
                resolved.version().getVariables());

        LlmCompletionResult completion = llmProvider.complete(new LlmCompletionRequest(
                system,
                user,
                resolved.template().getPurpose(),
                resolved.version().getExpectedOutputHint(),
                request.getVariables()));

        UUID executionId = UUID.randomUUID();
        eventPublisher.publish(PromptExecutedEvent.of(
                tenantId.toString(),
                executionId.toString(),
                resolved.template().getCode(),
                String.valueOf(resolved.version().getVersionNumber()),
                completion.provider(),
                completion.model(),
                completion.confidence() != null ? completion.confidence().toString() : null,
                request.getBusinessReference(),
                correlationId()));

        return AiExecuteResponse.builder()
                .executionId(executionId)
                .promptId(resolved.template().getId())
                .promptCode(resolved.template().getCode())
                .promptVersion(resolved.version().getVersionNumber())
                .provider(completion.provider())
                .model(completion.model())
                .renderedSystemPrompt(system)
                .renderedUserPrompt(user)
                .rawText(completion.rawText())
                .structuredOutput(completion.structuredOutput())
                .confidence(completion.confidence())
                .promptTokens(completion.promptTokens())
                .completionTokens(completion.completionTokens())
                .businessReference(request.getBusinessReference())
                .build();
    }

    private UUID requireTenantId() {
        String raw = TenantContext.getTenantId();
        if (!StringUtils.hasText(raw)) {
            throw new ValidationException("Tenant context is required");
        }
        return UUID.fromString(raw);
    }

    private String correlationId() {
        return CorrelationIdUtils.get().orElseGet(CorrelationIdUtils::generate);
    }
}
