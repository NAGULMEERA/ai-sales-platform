package com.aisales.ai.application.service;

import com.aisales.ai.application.rag.KnowledgeContextAssembler;
import com.aisales.ai.domain.cache.CachedLlmResponse;
import com.aisales.ai.domain.llm.LlmCompletionRequest;
import com.aisales.ai.domain.llm.LlmCompletionResult;
import com.aisales.ai.domain.llm.LlmProvider;
import com.aisales.ai.infrastructure.configuration.LlmProperties;
import com.aisales.common.contracts.ai.AiExecuteRequest;
import com.aisales.common.contracts.ai.AiExecuteResponse;
import com.aisales.common.contracts.ai.RetrievedKnowledgeChunkDto;
import com.aisales.common.core.util.CorrelationIdUtils;
import com.aisales.common.core.util.TenantContext;
import com.aisales.common.events.model.PromptExecutedEvent;
import com.aisales.common.events.publisher.EventPublisher;
import com.aisales.common.exception.exception.ValidationException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.StringUtils;

/**
 * AI Gateway entry point. Resolves versioned prompts, optionally retrieves knowledge (RAG),
 * enforces daily token quota, invokes a provider-abstracted LLM, records usage, and returns
 * structured output. Embedding / LLM remote calls run outside a DB transaction.
 */
@Slf4j
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
    private final SemanticCacheService semanticCacheService;
    private final LlmProperties llmProperties;
    private final PlatformTransactionManager transactionManager;

    public AiExecuteResponse execute(AiExecuteRequest request) {
        UUID tenantId = requireTenantId();

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

        String promptScope = promptScope(
                resolved.template().getCode(), resolved.version().getVersionNumber());
        String cacheQuery = cacheQueryKey(system, user);
        String cacheModel = resolveCacheModel();

        Optional<CachedLlmResponse> cached =
                semanticCacheService.get(tenantId, promptScope, cacheQuery, cacheModel);
        if (cached.isPresent()) {
            log.debug(
                    "ai_execute_cache_hit tenant_id={} prompt_code={} model={}",
                    tenantId,
                    resolved.template().getCode(),
                    cacheModel);
            return buildCachedResponse(tenantId, request, resolved, system, user, retrieved, cached.get());
        }

        long reserved = aiQuotaService.reserveExecute(tenantId);
        try {
            LlmCompletionResult completion = llmProvider.complete(new LlmCompletionRequest(
                    system,
                    user,
                    resolved.template().getPurpose(),
                    resolved.version().getExpectedOutputHint(),
                    variables));

            UUID executionId = UUID.randomUUID();
            persistUsageAndPublish(
                    tenantId,
                    executionId,
                    resolved,
                    request.getBusinessReference(),
                    completion.provider(),
                    completion.model(),
                    completion.confidence() != null ? completion.confidence().toString() : null,
                    completion.promptTokens(),
                    completion.completionTokens(),
                    () -> tokenUsageService.recordExecuteUsage(
                            tenantId,
                            executionId,
                            resolved.template().getCode(),
                            completion,
                            request.getBusinessReference()));

            semanticCacheService.put(
                    tenantId,
                    promptScope,
                    cacheQuery,
                    toCachedResponse(completion),
                    completion.model() != null ? completion.model() : cacheModel);

            return AiExecuteResponse.builder()
                    .executionId(executionId)
                    .promptId(resolved.template().getId())
                    .promptCode(resolved.template().getCode())
                    .promptVersion(resolved.version().getVersionNumber())
                    .provider(completion.provider())
                    .model(completion.model())
                    .renderedSystemPrompt(request.isIncludeRenderedPrompts() ? system : null)
                    .renderedUserPrompt(request.isIncludeRenderedPrompts() ? user : null)
                    .rawText(completion.rawText())
                    .structuredOutput(completion.structuredOutput())
                    .confidence(completion.confidence())
                    .promptTokens(completion.promptTokens())
                    .completionTokens(completion.completionTokens())
                    .businessReference(request.getBusinessReference())
                    .knowledgeBaseId(request.getKnowledgeBaseId())
                    .retrievedChunks(retrieved)
                    .build();
        } finally {
            aiQuotaService.release(tenantId, AiQuotaService.OPERATION_EXECUTE, reserved);
        }
    }

    private AiExecuteResponse buildCachedResponse(
            UUID tenantId,
            AiExecuteRequest request,
            PromptService.ResolvedPrompt resolved,
            String system,
            String user,
            List<RetrievedKnowledgeChunkDto> retrieved,
            CachedLlmResponse cached) {
        UUID executionId = UUID.randomUUID();
        Map<String, Object> metadata = cached.getMetadata() != null ? cached.getMetadata() : Map.of();
        String provider = stringMeta(metadata, "provider", "CACHE");
        Double confidence = doubleMeta(metadata, "confidence");
        @SuppressWarnings("unchecked")
        Map<String, Object> structured = metadata.get("structuredOutput") instanceof Map<?, ?> map
                ? new LinkedHashMap<>((Map<String, Object>) map)
                : new LinkedHashMap<>();

        persistUsageAndPublish(
                tenantId,
                executionId,
                resolved,
                request.getBusinessReference(),
                provider,
                cached.getModel(),
                confidence != null ? confidence.toString() : null,
                0,
                0,
                null);

        return AiExecuteResponse.builder()
                .executionId(executionId)
                .promptId(resolved.template().getId())
                .promptCode(resolved.template().getCode())
                .promptVersion(resolved.version().getVersionNumber())
                .provider(provider)
                .model(cached.getModel())
                .renderedSystemPrompt(request.isIncludeRenderedPrompts() ? system : null)
                .renderedUserPrompt(request.isIncludeRenderedPrompts() ? user : null)
                .rawText(cached.getContent())
                .structuredOutput(structured)
                .confidence(confidence)
                .promptTokens(0)
                .completionTokens(0)
                .businessReference(request.getBusinessReference())
                .knowledgeBaseId(request.getKnowledgeBaseId())
                .retrievedChunks(retrieved)
                .build();
    }

    /**
     * Usage ledger + outbox publish share one short TX (OutboxEventPublisher is MANDATORY).
     * LLM / embedding HTTP must stay outside this boundary.
     */
    private void persistUsageAndPublish(
            UUID tenantId,
            UUID executionId,
            PromptService.ResolvedPrompt resolved,
            String businessReference,
            String provider,
            String model,
            String confidence,
            Integer promptTokens,
            Integer completionTokens,
            Runnable usageRecorder) {
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            if (usageRecorder != null) {
                usageRecorder.run();
            }
            eventPublisher.publish(PromptExecutedEvent.of(
                    tenantId.toString(),
                    executionId.toString(),
                    resolved.template().getCode(),
                    String.valueOf(resolved.version().getVersionNumber()),
                    provider,
                    model,
                    confidence,
                    businessReference,
                    correlationId(),
                    promptTokens,
                    completionTokens));
        });
    }

    private static CachedLlmResponse toCachedResponse(LlmCompletionResult completion) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("provider", completion.provider());
        if (completion.confidence() != null) {
            metadata.put("confidence", completion.confidence());
        }
        if (completion.structuredOutput() != null) {
            metadata.put("structuredOutput", completion.structuredOutput());
        }
        if (completion.promptTokens() != null) {
            metadata.put("promptTokens", completion.promptTokens());
        }
        if (completion.completionTokens() != null) {
            metadata.put("completionTokens", completion.completionTokens());
        }
        return CachedLlmResponse.builder()
                .content(completion.rawText())
                .model(completion.model())
                .metadata(metadata)
                .build();
    }

    private static String promptScope(String promptCode, Integer version) {
        return (promptCode != null ? promptCode : "")
                + "|v"
                + (version != null ? version : 0);
    }

    private static String cacheQueryKey(String system, String user) {
        return "system="
                + (system != null ? system : "")
                + "|user="
                + (user != null ? user : "");
    }

    private String resolveCacheModel() {
        String provider = llmProvider.name();
        if (!StringUtils.hasText(provider)) {
            return "unknown";
        }
        return switch (provider.trim().toUpperCase(Locale.ROOT)) {
            case "OPENAI" -> llmProperties.getOpenai().getModel();
            case "GEMINI" -> llmProperties.getGemini().getModel();
            case "STUB" -> "stub-model";
            default -> provider.trim().toLowerCase(Locale.ROOT);
        };
    }

    private static String stringMeta(Map<String, Object> metadata, String key, String defaultValue) {
        Object value = metadata.get(key);
        return value != null ? String.valueOf(value) : defaultValue;
    }

    private static Double doubleMeta(Map<String, Object> metadata, String key) {
        Object value = metadata.get(key);
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        if (value instanceof String text && StringUtils.hasText(text)) {
            try {
                return Double.parseDouble(text);
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
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
