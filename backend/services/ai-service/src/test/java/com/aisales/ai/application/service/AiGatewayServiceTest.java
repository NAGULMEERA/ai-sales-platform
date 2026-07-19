package com.aisales.ai.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.aisales.ai.application.rag.KnowledgeContextAssembler;
import com.aisales.ai.domain.cache.CachedLlmResponse;
import com.aisales.ai.domain.entity.PromptTemplate;
import com.aisales.ai.domain.entity.PromptVersionEntity;
import com.aisales.ai.domain.llm.LlmCompletionResult;
import com.aisales.ai.domain.llm.LlmProvider;
import com.aisales.ai.infrastructure.configuration.LlmProperties;
import com.aisales.common.contracts.ai.AiExecuteRequest;
import com.aisales.common.contracts.ai.AiExecuteResponse;
import com.aisales.common.contracts.ai.PromptStatus;
import com.aisales.common.contracts.ai.RetrievedKnowledgeChunkDto;
import com.aisales.common.core.util.TenantContext;
import com.aisales.common.events.model.PromptExecutedEvent;
import com.aisales.common.events.publisher.EventPublisher;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.SimpleTransactionStatus;

@ExtendWith(MockitoExtension.class)
class AiGatewayServiceTest {

    @Mock private PromptService promptService;
    @Mock private LlmProvider llmProvider;
    @Mock private EventPublisher eventPublisher;
    @Mock private KnowledgeRetrievalService knowledgeRetrievalService;
    @Mock private AiQuotaService aiQuotaService;
    @Mock private TokenUsageService tokenUsageService;
    @Mock private SemanticCacheService semanticCacheService;
    @Mock private PlatformTransactionManager transactionManager;

    private AiGatewayService aiGatewayService;
    private UUID tenantId;

    @BeforeEach
    void setUp() {
        tenantId = UUID.randomUUID();
        TenantContext.setTenantId(tenantId.toString());
        LlmProperties llmProperties = new LlmProperties();
        llmProperties.setProvider("STUB");
        lenient().when(llmProvider.name()).thenReturn("STUB");
        lenient().when(semanticCacheService.get(any(), anyString(), anyString(), anyString()))
                .thenReturn(Optional.empty());
        lenient().when(aiQuotaService.reserveExecute(any())).thenReturn(4096L);
        lenient().when(transactionManager.getTransaction(any())).thenReturn(new SimpleTransactionStatus());
        aiGatewayService = new AiGatewayService(
                promptService,
                new PromptRenderer(),
                llmProvider,
                eventPublisher,
                knowledgeRetrievalService,
                new KnowledgeContextAssembler(),
                aiQuotaService,
                tokenUsageService,
                semanticCacheService,
                llmProperties,
                transactionManager);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void shouldExecutePromptAndPublishEvent() {
        UUID promptId = UUID.randomUUID();
        PromptTemplate template = PromptTemplate.builder()
                .id(promptId)
                .tenantId(tenantId)
                .code("LEAD_QUALIFY")
                .name("Lead Qualify")
                .purpose("LEAD_QUALIFICATION")
                .status(PromptStatus.ACTIVE)
                .activeVersion(1)
                .createdAt(java.time.Instant.now())
                .updatedAt(java.time.Instant.now())
                .build();
        PromptVersionEntity version = PromptVersionEntity.builder()
                .id(UUID.randomUUID())
                .tenantId(tenantId)
                .promptId(promptId)
                .versionNumber(1)
                .systemTemplate("You are a qualifier")
                .userTemplate("Qualify {{leadName}}")
                .variables(List.of("leadName"))
                .status(PromptStatus.ACTIVE)
                .createdAt(java.time.Instant.now())
                .build();
        when(promptService.resolveForExecution("LEAD_QUALIFY", null, null))
                .thenReturn(new PromptService.ResolvedPrompt(template, version));

        Map<String, Object> structured = new LinkedHashMap<>();
        structured.put("recommendation", "REVIEW");
        when(llmProvider.complete(any())).thenReturn(new LlmCompletionResult(
                "STUB", "stub-model", "{\"recommendation\":\"REVIEW\"}", structured, 0.85, 10, 5));

        AiExecuteResponse response = aiGatewayService.execute(AiExecuteRequest.builder()
                .promptCode("LEAD_QUALIFY")
                .variables(Map.of("leadName", "Jane"))
                .businessReference("lead-1")
                .build());

        assertThat(response.getPromptCode()).isEqualTo("LEAD_QUALIFY");
        assertThat(response.getRenderedUserPrompt()).isNull();
        assertThat(response.getRenderedSystemPrompt()).isNull();
        assertThat(response.getProvider()).isEqualTo("STUB");
        assertThat(response.getStructuredOutput()).containsEntry("recommendation", "REVIEW");
        assertThat(response.getConfidence()).isEqualTo(0.85);

        ArgumentCaptor<PromptExecutedEvent> captor = ArgumentCaptor.forClass(PromptExecutedEvent.class);
        verify(eventPublisher).publish(captor.capture());
        assertThat(captor.getValue().getEventType()).isEqualTo("PromptExecuted");
        assertThat(captor.getValue().getBusinessReference()).isEqualTo("lead-1");
        assertThat(captor.getValue().getPromptTokens()).isEqualTo(10);
        assertThat(captor.getValue().getCompletionTokens()).isEqualTo(5);
        assertThat(captor.getValue().getTotalTokens()).isEqualTo(15);
        verify(aiQuotaService).reserveExecute(tenantId);
        verify(aiQuotaService).release(eq(tenantId), eq(AiQuotaService.OPERATION_EXECUTE), eq(4096L));
        verify(tokenUsageService).recordExecuteUsage(
                eq(tenantId), any(UUID.class), eq("LEAD_QUALIFY"), any(), eq("lead-1"));
        verify(semanticCacheService)
                .put(eq(tenantId), eq("LEAD_QUALIFY|v1"), anyString(), any(CachedLlmResponse.class), eq("stub-model"));
    }

    @Test
    void shouldReturnCachedResponseWithoutCallingLlm() {
        stubPrompt("LEAD_QUALIFY", "Qualify {{leadName}}", List.of("leadName"));
        when(semanticCacheService.get(eq(tenantId), eq("LEAD_QUALIFY|v1"), anyString(), eq("stub-model")))
                .thenReturn(Optional.of(CachedLlmResponse.builder()
                        .content("{\"recommendation\":\"REVIEW\"}")
                        .model("stub-model")
                        .metadata(Map.of(
                                "provider", "STUB",
                                "confidence", 0.91,
                                "structuredOutput", Map.of("recommendation", "REVIEW")))
                        .build()));

        AiExecuteResponse response = aiGatewayService.execute(AiExecuteRequest.builder()
                .promptCode("LEAD_QUALIFY")
                .variables(Map.of("leadName", "Jane"))
                .businessReference("lead-cached")
                .build());

        assertThat(response.getProvider()).isEqualTo("STUB");
        assertThat(response.getRawText()).contains("REVIEW");
        assertThat(response.getStructuredOutput()).containsEntry("recommendation", "REVIEW");
        assertThat(response.getPromptTokens()).isZero();
        assertThat(response.getCompletionTokens()).isZero();
        verify(llmProvider, never()).complete(any());
        verify(tokenUsageService, never()).recordExecuteUsage(any(), any(), any(), any(), any());
        verify(aiQuotaService, never()).reserveExecute(any());
        verify(semanticCacheService, never()).put(any(), anyString(), anyString(), any(), anyString());
    }

    @Test
    void shouldInjectRetrievedKnowledgeIntoPromptWhenKnowledgeBaseSet() {
        UUID knowledgeBaseId = UUID.randomUUID();
        stubPrompt("FAQ_ANSWER", "Answer the customer.", List.of());

        when(knowledgeRetrievalService.resolveQuery(eq("warranty policy"), any()))
                .thenReturn("warranty policy");
        when(knowledgeRetrievalService.retrieve(eq(knowledgeBaseId), eq("warranty policy"), eq(3)))
                .thenReturn(List.of(RetrievedKnowledgeChunkDto.builder()
                        .chunkId(UUID.randomUUID())
                        .documentId(UUID.randomUUID())
                        .chunkIndex(0)
                        .content("Warranty covers 3 years or 36000 miles.")
                        .distance(0.12)
                        .build()));

        when(llmProvider.complete(any())).thenReturn(new LlmCompletionResult(
                "STUB", "stub-model", "{\"answer\":\"3 years\"}", Map.of("answer", "3 years"), 0.9, 20, 8));

        AiExecuteResponse response = aiGatewayService.execute(AiExecuteRequest.builder()
                .promptCode("FAQ_ANSWER")
                .knowledgeBaseId(knowledgeBaseId)
                .retrievalQuery("warranty policy")
                .retrievalTopK(3)
                .includeRenderedPrompts(true)
                .build());

        assertThat(response.getKnowledgeBaseId()).isEqualTo(knowledgeBaseId);
        assertThat(response.getRetrievedChunks()).hasSize(1);
        assertThat(response.getRenderedUserPrompt())
                .contains("## Retrieved knowledge")
                .contains("Warranty covers 3 years");
    }

    @Test
    void shouldQualifyRealEstateLeadViaSameExecuteApi() {
        stubPrompt(
                "LEAD_QUALIFY_REAL_ESTATE",
                "Qualify RE lead {{leadName}} budget={{budget}} location={{location}} timeline={{timeline}}",
                List.of("leadName", "budget", "location", "timeline"));

        when(llmProvider.complete(any())).thenReturn(new LlmCompletionResult(
                "STUB", "stub-model", "{\"recommendation\":\"QUALIFY\",\"score\":82}",
                Map.of("recommendation", "QUALIFY", "score", 82,
                        "inputs", Map.of("budget", "7500000", "location", "Whitefield", "timeline", "3 months")),
                0.9, 12, 6));

        AiExecuteResponse response = aiGatewayService.execute(AiExecuteRequest.builder()
                .promptCode("LEAD_QUALIFY_REAL_ESTATE")
                .variables(Map.of(
                        "leadName", "RE Buyer",
                        "budget", "7500000",
                        "location", "Whitefield",
                        "timeline", "3 months"))
                .businessReference("lead-re-1")
                .includeRenderedPrompts(true)
                .build());

        assertThat(response.getPromptCode()).isEqualTo("LEAD_QUALIFY_REAL_ESTATE");
        assertThat(response.getRenderedUserPrompt())
                .contains("budget=7500000")
                .contains("location=Whitefield")
                .contains("timeline=3 months");
        assertThat(response.getStructuredOutput()).containsEntry("recommendation", "QUALIFY");
    }

    @Test
    void shouldQualifyAutomobileLeadViaSameExecuteApi() {
        stubPrompt(
                "LEAD_QUALIFY_AUTOMOBILE",
                "Qualify Auto lead {{leadName}} budget={{budget}} vehicle={{vehicle}} finance={{financeRequired}} exchange={{exchange}}",
                List.of("leadName", "budget", "vehicle", "financeRequired", "exchange"));

        when(llmProvider.complete(any())).thenReturn(new LlmCompletionResult(
                "STUB", "stub-model", "{\"recommendation\":\"QUALIFY\",\"score\":78}",
                Map.of("recommendation", "QUALIFY", "score", 78,
                        "inputs", Map.of("budget", "1800000", "vehicle", "SUV",
                                "financeRequired", "true", "exchange", "yes")),
                0.88, 14, 7));

        AiExecuteResponse response = aiGatewayService.execute(AiExecuteRequest.builder()
                .promptCode("LEAD_QUALIFY_AUTOMOBILE")
                .variables(Map.of(
                        "leadName", "Auto Buyer",
                        "budget", "1800000",
                        "vehicle", "SUV",
                        "financeRequired", "true",
                        "exchange", "yes"))
                .businessReference("lead-auto-1")
                .includeRenderedPrompts(true)
                .build());

        assertThat(response.getPromptCode()).isEqualTo("LEAD_QUALIFY_AUTOMOBILE");
        assertThat(response.getRenderedUserPrompt())
                .contains("vehicle=SUV")
                .contains("finance=true")
                .contains("exchange=yes");
        assertThat(response.getStructuredOutput()).containsEntry("recommendation", "QUALIFY");
    }

    private void stubPrompt(String code, String userTemplate, List<String> variables) {
        UUID promptId = UUID.randomUUID();
        PromptTemplate template = PromptTemplate.builder()
                .id(promptId)
                .tenantId(tenantId)
                .code(code)
                .name(code)
                .purpose("LEAD_QUALIFICATION")
                .status(PromptStatus.ACTIVE)
                .activeVersion(1)
                .createdAt(java.time.Instant.now())
                .updatedAt(java.time.Instant.now())
                .build();
        PromptVersionEntity version = PromptVersionEntity.builder()
                .id(UUID.randomUUID())
                .tenantId(tenantId)
                .promptId(promptId)
                .versionNumber(1)
                .systemTemplate("You are a lead qualifier. Return JSON recommendation and score.")
                .userTemplate(userTemplate)
                .variables(variables)
                .status(PromptStatus.ACTIVE)
                .createdAt(java.time.Instant.now())
                .build();
        when(promptService.resolveForExecution(code, null, null))
                .thenReturn(new PromptService.ResolvedPrompt(template, version));
    }
}
