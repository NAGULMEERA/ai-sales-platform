package com.aisales.integration.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.aisales.common.contracts.client.LeadServiceClient;
import com.aisales.common.contracts.lead.CreateLeadRequest;
import com.aisales.common.contracts.lead.LeadDto;
import com.aisales.common.core.dto.ApiResponse;
import com.aisales.integration.application.dto.StubLeadAdsPayload;
import com.aisales.integration.domain.entity.MetaPageBinding;
import com.aisales.integration.infrastructure.configuration.MetaLeadAdsProperties;
import com.aisales.integration.infrastructure.meta.MetaGraphLeadClient;
import com.aisales.integration.infrastructure.persistence.IntegrationWebhookEventRepository;
import com.aisales.integration.infrastructure.persistence.MetaPageBindingRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import java.util.Optional;
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
class MetaLeadAdsWebhookServiceTest {

    @Mock private MetaPageBindingRepository metaPageBindingRepository;
    @Mock private IntegrationWebhookEventRepository webhookEventRepository;
    @Mock private LeadServiceClient leadServiceClient;
    @Mock private InstantVoiceQualifyService instantVoiceQualifyService;
    @Mock private MetaGraphLeadClient metaGraphLeadClient;

    private MetaLeadAdsProperties properties;
    private MetaLeadAdsWebhookService service;
    private UUID tenantId;
    private UUID leadId;

    @BeforeEach
    void setUp() {
        tenantId = UUID.randomUUID();
        leadId = UUID.randomUUID();
        properties = new MetaLeadAdsProperties();
        properties.setEnabled(true);
        properties.setMode("STUB");
        properties.setVerifyToken("dev-verify-token");
        service = new MetaLeadAdsWebhookService(
                properties,
                metaPageBindingRepository,
                webhookEventRepository,
                leadServiceClient,
                instantVoiceQualifyService,
                metaGraphLeadClient,
                new ObjectMapper(),
                passthroughTransactionManager());
    }

    @Test
    void shouldCreateLeadAndRunVoiceQualify() {
        when(webhookEventRepository.insertIgnoreConflict(any(), any(), any(), any())).thenReturn(1);
        when(metaPageBindingRepository.findByPageIdAndActiveTrue("page_1"))
                .thenReturn(Optional.of(binding("page_1")));
        when(leadServiceClient.createLead(any())).thenReturn(ApiResponse.ok(LeadDto.builder()
                .id(leadId)
                .tenantId(tenantId)
                .customerName("Ravi Kumar")
                .phone("+919999999999")
                .sourceType("FACEBOOK_LEAD_ADS")
                .build()));
        when(instantVoiceQualifyService.qualify(eq(leadId), any(), any()))
                .thenReturn(LeadDto.builder()
                        .id(leadId)
                        .attributes(Map.of("budget", "50-80L", "location", "Bengaluru"))
                        .build());

        String body = """
                {"pageId":"page_1","leadgenId":"lg_1","platform":"FACEBOOK","fullName":"Ravi Kumar","phone":"+919999999999","fields":{"budget":"80L"}}
                """;

        LeadDto result = service.handle(body, null);

        assertThat(result.getId()).isEqualTo(leadId);
        ArgumentCaptor<CreateLeadRequest> captor = ArgumentCaptor.forClass(CreateLeadRequest.class);
        verify(leadServiceClient).createLead(captor.capture());
        assertThat(captor.getValue().getSourceType()).isEqualTo("FACEBOOK_LEAD_ADS");
        assertThat(captor.getValue().getSourceId()).isEqualTo("lg_1");
        verify(instantVoiceQualifyService).qualify(eq(leadId), any(), any());
        verifyNoInteractions(metaGraphLeadClient);
    }

    @Test
    void shouldFetchLeadFromGraphInLiveMode() {
        properties.setMode("LIVE");
        properties.setAppSecret("secret");
        String envelope = """
                {"object":"page","entry":[{"id":"page_1","changes":[{"field":"leadgen","value":{"leadgen_id":"lg_live","page_id":"page_1"}}]}]}
                """;
        String signature = "sha256="
                + com.aisales.integration.infrastructure.meta.MetaWebhookSignatureVerifier.hmacSha256Hex(
                        "secret", envelope);

        when(metaPageBindingRepository.findByPageIdAndActiveTrue("page_1"))
                .thenReturn(Optional.of(binding("page_1")));
        when(metaGraphLeadClient.fetchLead("lg_live", "page_1", null))
                .thenReturn(StubLeadAdsPayload.builder()
                        .pageId("page_1")
                        .leadgenId("lg_live")
                        .platform("ig")
                        .fullName("Anita")
                        .phone("+918888888888")
                        .fields(Map.of("budget", "60L", "location", "Whitefield"))
                        .build());
        when(webhookEventRepository.insertIgnoreConflict(any(), any(), any(), any())).thenReturn(1);
        when(leadServiceClient.createLead(any())).thenReturn(ApiResponse.ok(LeadDto.builder()
                .id(leadId)
                .tenantId(tenantId)
                .build()));
        when(instantVoiceQualifyService.qualify(eq(leadId), any(), any()))
                .thenReturn(LeadDto.builder().id(leadId).build());

        LeadDto result = service.handle(envelope, signature);

        assertThat(result.getId()).isEqualTo(leadId);
        verify(metaGraphLeadClient).fetchLead("lg_live", "page_1", null);
        ArgumentCaptor<CreateLeadRequest> captor = ArgumentCaptor.forClass(CreateLeadRequest.class);
        verify(leadServiceClient).createLead(captor.capture());
        assertThat(captor.getValue().getSourceType()).isEqualTo("INSTAGRAM_LEAD_ADS");
        assertThat(captor.getValue().getCustomerName()).isEqualTo("Anita");
    }

    @Test
    void shouldSkipDuplicateLeadgen() {
        when(metaPageBindingRepository.findByPageIdAndActiveTrue("page_1"))
                .thenReturn(Optional.of(binding("page_1")));
        when(webhookEventRepository.insertIgnoreConflict(any(), any(), any(), any())).thenReturn(0);

        String body = """
                {"pageId":"page_1","leadgenId":"lg_dup","fullName":"A","phone":"1"}
                """;

        LeadDto result = service.handle(body, null);

        assertThat(result).isNull();
        verifyNoInteractions(leadServiceClient);
        verifyNoInteractions(instantVoiceQualifyService);
    }

    @Test
    void shouldVerifySubscriptionChallenge() {
        String challenge = service.verifySubscription("subscribe", "dev-verify-token", "12345");
        assertThat(challenge).isEqualTo("12345");
    }

    private MetaPageBinding binding(String pageId) {
        return MetaPageBinding.builder()
                .id(UUID.randomUUID())
                .pageId(pageId)
                .tenantId(tenantId)
                .sourceType("FACEBOOK_LEAD_ADS")
                .promptCode("LEAD_QUALIFY_REAL_ESTATE")
                .qualificationVariableKeys("budget,location,timeline")
                .voiceQualifyEnabled(true)
                .active(true)
                .build();
    }

    private static PlatformTransactionManager passthroughTransactionManager() {
        return new PlatformTransactionManager() {
            @Override
            public TransactionStatus getTransaction(TransactionDefinition definition) {
                return new SimpleTransactionStatus(true);
            }

            @Override
            public void commit(TransactionStatus status) {
            }

            @Override
            public void rollback(TransactionStatus status) {
            }
        };
    }
}
