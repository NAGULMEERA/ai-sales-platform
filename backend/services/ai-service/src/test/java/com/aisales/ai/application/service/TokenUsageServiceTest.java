package com.aisales.ai.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.aisales.ai.domain.entity.TokenUsage;
import com.aisales.ai.domain.llm.LlmCompletionResult;
import com.aisales.ai.infrastructure.configuration.AiCostProperties;
import com.aisales.ai.infrastructure.persistence.TokenUsageRepository;
import com.aisales.common.core.util.TenantContext;
import com.aisales.common.observability.metrics.AiRequestMetrics;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;

@ExtendWith(MockitoExtension.class)
class TokenUsageServiceTest {

    @Mock private TokenUsageRepository tokenUsageRepository;
    @Mock private ObjectProvider<AiRequestMetrics> aiRequestMetrics;
    @Mock private AiRequestMetrics metrics;

    private TokenUsageService service;
    private UUID tenantId;

    @BeforeEach
    void setUp() {
        tenantId = UUID.randomUUID();
        TenantContext.setTenantId(tenantId.toString());
        when(aiRequestMetrics.getIfAvailable()).thenReturn(metrics);
        when(tokenUsageRepository.save(any(TokenUsage.class))).thenAnswer(inv -> inv.getArgument(0));

        AiCostProperties costProperties = new AiCostProperties();
        costProperties.setEnabled(true);
        AiCostProperties.ModelRate llm = new AiCostProperties.ModelRate();
        llm.setPromptPer1k(new BigDecimal("0.001"));
        llm.setCompletionPer1k(new BigDecimal("0.002"));
        costProperties.getModels().put("stub-model", llm);
        AiCostProperties.ModelRate embed = new AiCostProperties.ModelRate();
        embed.setEmbeddingPer1k(new BigDecimal("0.01"));
        costProperties.getModels().put("stub-embedding-1024", embed);

        service = new TokenUsageService(
                tokenUsageRepository, new AiCostEstimator(costProperties), aiRequestMetrics);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void shouldPersistLlmTokensWithCostEstimate() {
        UUID executionId = UUID.randomUUID();
        LlmCompletionResult completion = new LlmCompletionResult(
                "STUB", "stub-model", "{}", Map.of(), 0.8, 1000, 500);

        TokenUsage usage = service.recordExecuteUsage(
                tenantId, executionId, "LEAD_QUALIFY", completion, "lead-1");

        assertThat(usage.getTotalTokens()).isEqualTo(1500);
        assertThat(usage.getEstimatedCostUsd()).isEqualByComparingTo("0.00200000");
        verify(metrics).recordTokens(tenantId.toString(), "STUB", "EXECUTE", 1500);
    }

    @Test
    void shouldPersistEmbeddingUsage() {
        TokenUsage usage = service.recordEmbeddingUsage(
                tenantId,
                "OPEN_SOURCE",
                "stub-embedding-1024",
                List.of("a".repeat(4000)),
                "doc-1",
                "RAG_INDEX");

        assertThat(usage.getOperation()).isEqualTo("EMBED");
        assertThat(usage.getTotalTokens()).isEqualTo(1000);
        assertThat(usage.getEstimatedCostUsd()).isEqualByComparingTo("0.01000000");
        verify(metrics).recordTokens(tenantId.toString(), "OPEN_SOURCE", "EMBED", 1000);

        ArgumentCaptor<TokenUsage> captor = ArgumentCaptor.forClass(TokenUsage.class);
        verify(tokenUsageRepository).save(captor.capture());
        assertThat(captor.getValue().getBusinessReference()).isEqualTo("doc-1");
    }

    @Test
    void shouldPreferProviderReportedEmbeddingTokens() {
        TokenUsage usage = service.recordEmbeddingUsage(
                tenantId,
                "OPENAI",
                "text-embedding-3-small",
                List.of("short text"),
                "doc-2",
                "EMBED",
                42);

        assertThat(usage.getTotalTokens()).isEqualTo(42);
        assertThat(usage.getPromptTokens()).isEqualTo(42);
    }
}
