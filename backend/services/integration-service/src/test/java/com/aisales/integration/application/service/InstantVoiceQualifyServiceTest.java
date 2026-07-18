package com.aisales.integration.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.aisales.common.contracts.client.LeadServiceClient;
import com.aisales.common.contracts.lead.AiLeadQualificationResultDto;
import com.aisales.common.contracts.lead.LeadDto;
import com.aisales.common.contracts.lead.QualifyLeadWithAiRequest;
import com.aisales.common.contracts.lead.UpdateLeadRequest;
import com.aisales.common.core.dto.ApiResponse;
import com.aisales.integration.domain.entity.MetaPageBinding;
import com.aisales.integration.domain.entity.VoiceCall;
import com.aisales.integration.domain.voice.VoiceProvider;
import com.aisales.integration.domain.voice.VoiceProvider.VoiceCallRequest;
import com.aisales.integration.domain.voice.VoiceProvider.VoiceCallResult;
import com.aisales.integration.infrastructure.persistence.VoiceCallRepository;
import com.aisales.integration.infrastructure.voice.VoiceProviderRegistry;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.SimpleTransactionStatus;

@ExtendWith(MockitoExtension.class)
class InstantVoiceQualifyServiceTest {

    @Mock private LeadServiceClient leadServiceClient;
    @Mock private VoiceProviderRegistry voiceProviderRegistry;
    @Mock private VoiceCallRepository voiceCallRepository;
    @Mock private PlatformTransactionManager transactionManager;
    @Mock private VoiceProvider voiceProvider;

    private InstantVoiceQualifyService service;
    private UUID leadId;
    private UUID tenantId;

    @BeforeEach
    void setUp() {
        leadId = UUID.randomUUID();
        tenantId = UUID.randomUUID();
        service = new InstantVoiceQualifyService(
                leadServiceClient, voiceProviderRegistry, voiceCallRepository, transactionManager);
        when(transactionManager.getTransaction(any(TransactionDefinition.class)))
                .thenReturn(new SimpleTransactionStatus());
    }

    @Test
    void shouldFillStubVoiceAttributesAndAiQualify() {
        when(leadServiceClient.getLead(leadId))
                .thenReturn(ApiResponse.ok(LeadDto.builder()
                        .id(leadId)
                        .tenantId(tenantId)
                        .phone("+919999999999")
                        .customerName("Ada")
                        .build()));
        when(voiceProviderRegistry.resolveDefault()).thenReturn(voiceProvider);
        when(voiceProvider.name()).thenReturn("STUB");
        when(voiceProvider.placeOutboundCall(any(VoiceCallRequest.class)))
                .thenReturn(VoiceCallResult.completed(
                        "stub_" + leadId,
                        Map.of("budget", "50-80L", "location", "Bengaluru", "timeline", "3-6 months")));
        when(voiceCallRepository.save(any(VoiceCall.class))).thenAnswer(inv -> inv.getArgument(0));
        when(leadServiceClient.updateLead(eq(leadId), any()))
                .thenReturn(ApiResponse.ok(LeadDto.builder().id(leadId).build()));
        when(leadServiceClient.contactLead(leadId, "VOICE"))
                .thenReturn(ApiResponse.ok(LeadDto.builder().id(leadId).build()));
        when(leadServiceClient.qualifyWithAi(eq(leadId), any()))
                .thenReturn(ApiResponse.ok(AiLeadQualificationResultDto.builder()
                        .lead(LeadDto.builder()
                                .id(leadId)
                                .qualified(true)
                                .attributes(Map.of("budget", "50-80L"))
                                .build())
                        .recommendation("QUALIFY")
                        .qualified(true)
                        .build()));

        MetaPageBinding binding = MetaPageBinding.builder()
                .promptCode("LEAD_QUALIFY_REAL_ESTATE")
                .qualificationVariableKeys("budget,location,timeline")
                .build();

        LeadDto result = service.qualify(leadId, binding, Map.of());

        assertThat(result.isQualified()).isTrue();
        ArgumentCaptor<UpdateLeadRequest> updateCaptor = ArgumentCaptor.forClass(UpdateLeadRequest.class);
        verify(leadServiceClient).updateLead(eq(leadId), updateCaptor.capture());
        assertThat(updateCaptor.getValue().getAttributes())
                .containsKeys("budget", "location", "timeline", "voiceCallStatus", "voiceChannel", "voiceCallId");
        ArgumentCaptor<QualifyLeadWithAiRequest> aiCaptor =
                ArgumentCaptor.forClass(QualifyLeadWithAiRequest.class);
        verify(leadServiceClient).qualifyWithAi(eq(leadId), aiCaptor.capture());
        assertThat(aiCaptor.getValue().getPromptCode()).isEqualTo("LEAD_QUALIFY_REAL_ESTATE");
        assertThat(aiCaptor.getValue().getVariableKeys()).containsExactly("budget", "location", "timeline");
        verify(voiceCallRepository).save(any(VoiceCall.class));
        verify(transactionManager).commit(any(TransactionStatus.class));
    }
}
