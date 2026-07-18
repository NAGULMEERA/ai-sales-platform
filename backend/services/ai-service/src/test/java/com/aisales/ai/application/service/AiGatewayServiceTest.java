package com.aisales.ai.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.aisales.ai.domain.entity.PromptTemplate;
import com.aisales.ai.domain.entity.PromptVersionEntity;
import com.aisales.ai.domain.llm.LlmCompletionResult;
import com.aisales.ai.domain.llm.LlmProvider;
import com.aisales.common.contracts.ai.AiExecuteRequest;
import com.aisales.common.contracts.ai.AiExecuteResponse;
import com.aisales.common.contracts.ai.PromptStatus;
import com.aisales.common.core.util.TenantContext;
import com.aisales.common.events.model.PromptExecutedEvent;
import com.aisales.common.events.publisher.EventPublisher;
import java.util.LinkedHashMap;
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

@ExtendWith(MockitoExtension.class)
class AiGatewayServiceTest {

    @Mock private PromptService promptService;
    @Mock private LlmProvider llmProvider;
    @Mock private EventPublisher eventPublisher;

    private AiGatewayService aiGatewayService;
    private UUID tenantId;

    @BeforeEach
    void setUp() {
        tenantId = UUID.randomUUID();
        TenantContext.setTenantId(tenantId.toString());
        aiGatewayService = new AiGatewayService(
                promptService, new PromptRenderer(), llmProvider, eventPublisher);
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
        assertThat(response.getRenderedUserPrompt()).isEqualTo("Qualify Jane");
        assertThat(response.getProvider()).isEqualTo("STUB");
        assertThat(response.getStructuredOutput()).containsEntry("recommendation", "REVIEW");
        assertThat(response.getConfidence()).isEqualTo(0.85);

        ArgumentCaptor<PromptExecutedEvent> captor = ArgumentCaptor.forClass(PromptExecutedEvent.class);
        verify(eventPublisher).publish(captor.capture());
        assertThat(captor.getValue().getEventType()).isEqualTo("PromptExecuted");
        assertThat(captor.getValue().getBusinessReference()).isEqualTo("lead-1");
    }
}
