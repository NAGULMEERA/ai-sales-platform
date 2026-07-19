package com.aisales.ai.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.aisales.common.contracts.ai.AiExecuteRequest;
import com.aisales.common.contracts.ai.AiExecuteResponse;
import com.aisales.common.contracts.ai.QualificationResultDto;
import com.aisales.common.contracts.ai.QualifyLeadAiRequest;
import com.aisales.common.core.util.TenantContext;
import com.aisales.common.events.model.AiQualificationCompletedEvent;
import com.aisales.common.events.model.AiRecommendationGeneratedEvent;
import com.aisales.common.events.model.BaseEvent;
import com.aisales.common.events.publisher.EventPublisher;
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
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.SimpleTransactionStatus;

@ExtendWith(MockitoExtension.class)
class AiQualificationOrchestratorTest {

    @Mock private AiGatewayService aiGatewayService;
    @Mock private EventPublisher eventPublisher;
    @Mock private PlatformTransactionManager transactionManager;
    @Mock private ObjectProvider<?> platformMetrics;

    private AiQualificationOrchestrator orchestrator;
    private UUID tenantId;

    @BeforeEach
    void setUp() {
        tenantId = UUID.randomUUID();
        TenantContext.setTenantId(tenantId.toString());
        when(transactionManager.getTransaction(any())).thenReturn(new SimpleTransactionStatus());
        when(platformMetrics.getIfAvailable()).thenReturn(null);
        orchestrator = new AiQualificationOrchestrator(
                aiGatewayService,
                new QualificationResponseParser(),
                eventPublisher,
                transactionManager,
                (ObjectProvider) platformMetrics);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void shouldExecuteViaGatewayAndPublishQualificationEvents() {
        UUID executionId = UUID.randomUUID();
        when(aiGatewayService.execute(any(AiExecuteRequest.class))).thenReturn(AiExecuteResponse.builder()
                .executionId(executionId)
                .promptCode("LEAD_QUALIFY_REAL_ESTATE")
                .promptVersion(1)
                .provider("STUB")
                .model("stub-model")
                .confidence(0.9)
                .structuredOutput(Map.of(
                        "recommendation", "QUALIFY",
                        "qualificationScore", 88,
                        "recommendedSalesAction", "Schedule site visit"))
                .build());

        QualificationResultDto result = orchestrator.qualify(QualifyLeadAiRequest.builder()
                .promptCode("LEAD_QUALIFY_REAL_ESTATE")
                .leadId("lead-1")
                .variables(Map.of("budget", "7500000", "location", "Whitefield"))
                .build());

        assertThat(result.getRecommendation()).isEqualTo("QUALIFY");
        assertThat(result.getQualificationScore()).isEqualTo(88);

        ArgumentCaptor<AiExecuteRequest> requestCaptor = ArgumentCaptor.forClass(AiExecuteRequest.class);
        verify(aiGatewayService).execute(requestCaptor.capture());
        assertThat(requestCaptor.getValue().getCapability()).isEqualTo("LEAD_QUALIFICATION");
        assertThat(requestCaptor.getValue().getBusinessReference()).isEqualTo("lead-1");

        ArgumentCaptor<BaseEvent> eventCaptor = ArgumentCaptor.forClass(BaseEvent.class);
        verify(eventPublisher, atLeastOnce()).publish(eventCaptor.capture());
        assertThat(eventCaptor.getAllValues()).anyMatch(AiQualificationCompletedEvent.class::isInstance);
        assertThat(eventCaptor.getAllValues()).anyMatch(AiRecommendationGeneratedEvent.class::isInstance);
    }
}
