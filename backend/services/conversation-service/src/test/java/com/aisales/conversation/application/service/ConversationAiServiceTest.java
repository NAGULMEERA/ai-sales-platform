package com.aisales.conversation.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.aisales.common.contracts.ai.AiExecuteResponse;
import com.aisales.common.contracts.ai.ConversationSummaryDto;
import com.aisales.common.contracts.client.AiServiceClient;
import com.aisales.common.contracts.conversation.ConversationAiInsightsDto;
import com.aisales.common.contracts.conversation.ConversationChannel;
import com.aisales.common.contracts.conversation.ConversationContextDto;
import com.aisales.common.contracts.conversation.ConversationStatus;
import com.aisales.common.core.dto.ApiResponse;
import com.aisales.common.core.util.TenantContext;
import com.aisales.common.events.publisher.EventPublisher;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;

@ExtendWith(MockitoExtension.class)
class ConversationAiServiceTest {

    @Mock private ConversationService conversationService;
    @Mock private ConversationContextService contextService;
    @Mock private AiServiceClient aiServiceClient;
    @Mock private EventPublisher eventPublisher;
    @Mock private ObjectProvider<com.aisales.common.observability.metrics.PlatformMetrics> platformMetrics;
    @Mock private TransactionTemplate transactionTemplate;

    private ConversationAiService aiService;
    private UUID tenantId;
    private UUID conversationId;

    @BeforeEach
    void setUp() {
        tenantId = UUID.randomUUID();
        conversationId = UUID.randomUUID();
        TenantContext.setTenantId(tenantId.toString());
        doAnswer(invocation -> {
                    @SuppressWarnings("unchecked")
                    Consumer<TransactionStatus> action = invocation.getArgument(0);
                    action.accept(mock(TransactionStatus.class));
                    return null;
                })
                .when(transactionTemplate)
                .executeWithoutResult(any());
        aiService = new ConversationAiService(
                conversationService,
                contextService,
                aiServiceClient,
                eventPublisher,
                new ObjectMapper(),
                platformMetrics,
                transactionTemplate);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void shouldGenerateInsightsViaAiGatewayOnly() {
        when(contextService.loadContext(conversationId)).thenReturn(ConversationContextDto.builder()
                .conversationId(conversationId)
                .tenantId(tenantId)
                .channel(ConversationChannel.WEB)
                .status(ConversationStatus.OPEN)
                .previousMessages(List.of())
                .build());
        when(aiServiceClient.execute(any())).thenReturn(ApiResponse.ok(AiExecuteResponse.builder()
                .executionId(UUID.randomUUID())
                .rawText("Customer is interested")
                .confidence(0.91)
                .structuredOutput(Map.of(
                        "summary", "Interested buyer",
                        "sentiment", "POSITIVE",
                        "intent", "BUY",
                        "classification", "HOT",
                        "nextBestAction", "SCHEDULE_VISIT",
                        "replySuggestion", "Happy to schedule a visit."))
                .build()));

        ConversationAiInsightsDto insights = aiService.generateInsights(conversationId);

        assertThat(insights.getIntent()).isEqualTo("BUY");
        assertThat(insights.getReplySuggestion()).contains("visit");
        assertThat(insights.getSummary()).isInstanceOf(ConversationSummaryDto.class);
        verify(aiServiceClient).execute(any());
        verify(conversationService).applyAiInsights(
                conversationId, "Interested buyer", "POSITIVE", "BUY", "HOT", "SCHEDULE_VISIT");
    }
}
