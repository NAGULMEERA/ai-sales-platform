package com.aisales.lead.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.aisales.common.contracts.catalog.CatalogMatchCandidateDto;
import com.aisales.common.contracts.catalog.CatalogRecommendationResultDto;
import com.aisales.common.contracts.client.CatalogServiceClient;
import com.aisales.common.contracts.client.DealServiceClient;
import com.aisales.common.contracts.deal.CreateOpportunityRequest;
import com.aisales.common.contracts.deal.OpportunityDto;
import com.aisales.common.contracts.deal.OpportunityStatus;
import com.aisales.common.contracts.lead.ConvertLeadToOpportunityRequest;
import com.aisales.common.contracts.lead.LeadOpportunityConversionResultDto;
import com.aisales.common.contracts.lead.LeadStatus;
import com.aisales.common.core.dto.ApiResponse;
import com.aisales.common.core.util.TenantContext;
import com.aisales.lead.application.mapper.LeadMapper;
import com.aisales.lead.domain.entity.Lead;
import com.aisales.lead.infrastructure.persistence.LeadRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashMap;
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

@ExtendWith(MockitoExtension.class)
class LeadOpportunityConversionServiceTest {

    @Mock private LeadRepository leadRepository;
    @Mock private LeadService leadService;
    @Mock private LeadCustomerConversionGateway customerConversionGateway;
    @Mock private CatalogServiceClient catalogServiceClient;
    @Mock private DealServiceClient dealServiceClient;
    @Mock private LeadSideEffectRecorder sideEffects;

    private LeadOpportunityConversionService service;
    private UUID tenantId;
    private UUID leadId;
    private UUID customerId;

    @BeforeEach
    void setUp() {
        tenantId = UUID.randomUUID();
        leadId = UUID.randomUUID();
        customerId = UUID.randomUUID();
        TenantContext.setTenantId(tenantId.toString());
        service = new LeadOpportunityConversionService(
                leadRepository,
                leadService,
                customerConversionGateway,
                catalogServiceClient,
                dealServiceClient,
                sideEffects,
                new LeadMapper());
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void shouldCreateOpportunityFromCatalogRecommendation() {
        Lead lead = Lead.builder()
                .id(leadId)
                .tenantId(tenantId)
                .customerName("Jane")
                .phone("+919999999999")
                .sourceType("WEB")
                .status(LeadStatus.QUALIFIED)
                .customerId(customerId)
                .score(80)
                .attributes(new HashMap<>(Map.of("budget", 7500000, "location", "Whitefield")))
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
        when(leadRepository.findByTenantIdAndIdAndDeletedAtIsNull(tenantId, leadId))
                .thenReturn(Optional.of(lead));

        UUID productId = UUID.randomUUID();
        UUID offerId = UUID.randomUUID();
        when(catalogServiceClient.recommend(any())).thenReturn(ApiResponse.ok(
                CatalogRecommendationResultDto.builder()
                        .leadId(leadId)
                        .recommendations(List.of(CatalogMatchCandidateDto.builder()
                                .productId(productId)
                                .productCode("RE-3BHK")
                                .productName("Whitefield 3BHK")
                                .offerId(offerId)
                                .unitPrice(new BigDecimal("7000000"))
                                .currency("INR")
                                .matchScore(92)
                                .confidenceScore(85)
                                .build()))
                        .overallConfidence(0.85)
                        .build()));

        UUID opportunityId = UUID.randomUUID();
        when(dealServiceClient.createOpportunity(any())).thenReturn(ApiResponse.ok(
                OpportunityDto.builder()
                        .id(opportunityId)
                        .customerId(customerId)
                        .leadId(leadId)
                        .status(OpportunityStatus.OPEN)
                        .name("Whitefield 3BHK — Jane")
                        .build()));
        when(dealServiceClient.updateOpportunityStage(eq(opportunityId), any()))
                .thenReturn(ApiResponse.ok(OpportunityDto.builder()
                        .id(opportunityId)
                        .customerId(customerId)
                        .leadId(leadId)
                        .status(OpportunityStatus.QUALIFIED)
                        .name("Whitefield 3BHK — Jane")
                        .build()));
        when(dealServiceClient.scoreOpportunity(eq(opportunityId), any()))
                .thenReturn(ApiResponse.ok(OpportunityDto.builder()
                        .id(opportunityId)
                        .score(84)
                        .status(OpportunityStatus.QUALIFIED)
                        .build()));

        LeadOpportunityConversionResultDto result = service.convertToOpportunity(
                leadId, ConvertLeadToOpportunityRequest.builder().category("residential").build());

        assertThat(result.getOpportunity().getId()).isEqualTo(opportunityId);
        assertThat(result.getSelectedProductId()).isEqualTo(productId);
        assertThat(result.getSelectedOfferId()).isEqualTo(offerId);

        ArgumentCaptor<CreateOpportunityRequest> captor =
                ArgumentCaptor.forClass(CreateOpportunityRequest.class);
        verify(dealServiceClient).createOpportunity(captor.capture());
        assertThat(captor.getValue().getCatalogProductId()).isEqualTo(productId);
        assertThat(captor.getValue().getCustomerId()).isEqualTo(customerId);
        verify(sideEffects).recordActivity(eq(leadId), eq("OPPORTUNITY_CREATED"), any(), any());
        verify(sideEffects).recordActivity(eq(leadId), eq("CATALOG_MATCHED"), any(), any());
    }
}
