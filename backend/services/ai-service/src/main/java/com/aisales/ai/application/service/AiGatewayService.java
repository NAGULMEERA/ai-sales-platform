package com.aisales.ai.application.service;

import com.aisales.ai.application.rag.KnowledgeContextAssembler;
import com.aisales.ai.domain.llm.LlmCompletionRequest;
import com.aisales.ai.domain.llm.LlmCompletionResult;
import com.aisales.ai.domain.llm.LlmProvider;
import com.aisales.common.contracts.ai.AiExecuteRequest;
import com.aisales.common.contracts.ai.AiExecuteResponse;
import com.aisales.common.contracts.ai.RetrievedKnowledgeChunkDto;
import com.aisales.common.core.util.CorrelationIdUtils;
import com.aisales.common.core.util.TenantContext;
import com.aisales.common.events.model.PromptExecutedEvent;
import com.aisales.common.events.publisher.EventPublisher;
import com.aisales.common.exception.exception.ValidationException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * AI Gateway entry point. Resolves versioned prompts, optionally retrieves knowledge (RAG),
 * enforces daily token quota, invokes a provider-abstracted LLM, records usage, and returns
 * structured output. Embedding / LLM remote calls run outside a DB transaction.
 */
@Service
@RequiredArgsConstructor
public class AiGatewayService {

    private final PromptService promptService;
    private final PromptRenderer promptRenderer;
    private final LlmProvider llmProvider;
    private final EventPublisher eventPublisher;
    private final KnowledgeRetrievalService knowledgeRetrievalService;
    private final KnowledgeContextAssembler knowledgeContextAssembler;
    private final AiQuotaService aiQuotaService;
    private final TokenUsageService tokenUsageService;

    public AiExecuteResponse execute(AiExecuteRequest request) {
        UUID tenantId = requireTenantId();
        aiQuotaService.assertWithinDailyBudget(tenantId);

        PromptService.ResolvedPrompt resolved = promptService.resolveForExecution(
                request.getPromptCode(), request.getPromptId(), request.getPromptVersion());

        Map<String, String> variables = new HashMap<>();
        if (request.getVariables() != null) {
            variables.putAll(request.getVariables());
        }

        List<RetrievedKnowledgeChunkDto> retrieved = List.of();
        String assembledContext = "";
        if (request.getKnowledgeBaseId() != null) {
            String query = knowledgeRetrievalService.resolveQuery(
                    request.getRetrievalQuery(), variables);
            retrieved = knowledgeRetrievalService.retrieve(
                    request.getKnowledgeBaseId(), query, request.getRetrievalTopK());
            assembledContext = knowledgeContextAssembler.assemble(retrieved);
            variables.put(
                    "knowledge_context",
                    StringUtils.hasText(assembledContext) ? assembledContext : "(no knowledge retrieved)");
        }

        String system = promptRenderer.render(
                resolved.version().getSystemTemplate(),
                variables,
                List.of());
        String user = promptRenderer.render(
                resolved.version().getUserTemplate(),
                variables,
                resolved.version().getVariables());

        // Prompts that do not declare {{knowledge_context}} still receive retrieved context.
        String userTemplate = resolved.version().getUserTemplate();
        if (request.getKnowledgeBaseId() != null
                && StringUtils.hasText(assembledContext)
                && (userTemplate == null || !userTemplate.contains("knowledge_context"))) {
            user = user + "\n\n## Retrieved knowledge\n" + assembledContext;
        }

        LlmCompletionResult completion = llmProvider.complete(new LlmCompletionRequest(
                system,
                user,
                resolved.template().getPurpose(),
                resolved.version().getExpectedOutputHint(),
                variables));

        UUID executionId = UUID.randomUUID();
        tokenUsageService.recordExecuteUsage(
                tenantId,
                executionId,
                resolved.template().getCode(),
                completion,
                request.getBusinessReference());

        eventPublisher.publish(PromptExecutedEvent.of(
                tenantId.toString(),
                executionId.toString(),
                resolved.template().getCode(),
                String.valueOf(resolved.version().getVersionNumber()),
                completion.provider(),
                completion.model(),
                completion.confidence() != null ? completion.confidence().toString() : null,
                request.getBusinessReference(),
                correlationId(),
                completion.promptTokens(),
                completion.completionTokens()));

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
                .knowledgeBaseId(request.getKnowledgeBaseId())
                .retrievedChunks(retrieved)
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
