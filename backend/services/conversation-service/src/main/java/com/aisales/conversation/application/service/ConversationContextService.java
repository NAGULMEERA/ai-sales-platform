package com.aisales.conversation.application.service;

import com.aisales.common.contracts.catalog.CatalogMatchRequest;
import com.aisales.common.contracts.catalog.CatalogRecommendationRequest;
import com.aisales.common.contracts.catalog.CatalogRecommendationResultDto;
import com.aisales.common.contracts.client.CatalogServiceClient;
import com.aisales.common.contracts.client.CustomerServiceClient;
import com.aisales.common.contracts.client.DealServiceClient;
import com.aisales.common.contracts.client.LeadServiceClient;
import com.aisales.common.contracts.conversation.ConversationContextDto;
import com.aisales.common.contracts.conversation.ConversationMessageDto;
import com.aisales.common.contracts.customer.CustomerDto;
import com.aisales.common.contracts.deal.OpportunityDto;
import com.aisales.common.contracts.lead.LeadDto;
import com.aisales.common.core.dto.ApiResponse;
import com.aisales.common.core.dto.PageResponse;
import com.aisales.common.contracts.ai.ConversationSummaryDto;
import com.aisales.conversation.application.mapper.ConversationMapper;
import com.aisales.conversation.domain.entity.ConversationThread;
import com.aisales.conversation.infrastructure.persistence.ConversationMessageRepository;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ConversationContextService {

    private final ConversationService conversationService;
    private final ConversationMessageRepository messageRepository;
    private final ConversationMapper mapper;
    private final ObjectProvider<LeadServiceClient> leadServiceClient;
    private final ObjectProvider<CustomerServiceClient> customerServiceClient;
    private final ObjectProvider<DealServiceClient> dealServiceClient;
    private final ObjectProvider<CatalogServiceClient> catalogServiceClient;

    /**
     * Loads local conversation state then enriches via Feign. No class-level transaction —
     * repository methods use short TX boundaries so remote calls do not hold JDBC connections.
     */
    public ConversationContextDto loadContext(UUID conversationId) {
        ConversationThread thread = conversationService.requireThread(conversationId);
        List<ConversationMessageDto> messages = messageRepository
                .findByTenantIdAndConversationIdOrderByCreatedAtAsc(thread.getTenantId(), conversationId)
                .stream()
                .map(mapper::toMessageDto)
                .toList();

        LeadDto lead = loadLead(thread.getLeadId());
        CustomerDto customer = loadCustomer(thread.getCustomerId(), thread.getLeadId());
        OpportunityDto opportunity = loadOpportunity(thread.getOpportunityId(), thread.getLeadId());
        CatalogRecommendationResultDto recommendation = loadCatalogRecommendation(thread, lead);

        ConversationSummaryDto aiSummary = null;
        if (thread.getAiSummary() != null || thread.getSentiment() != null || thread.getIntent() != null) {
            aiSummary = ConversationSummaryDto.builder()
                    .summary(thread.getAiSummary())
                    .sentiment(thread.getSentiment())
                    .intent(thread.getIntent())
                    .build();
        }

        Map<String, Object> tenantConfiguration = new HashMap<>();
        tenantConfiguration.put("channel", thread.getChannel().name());
        if (thread.getOrganizationId() != null) {
            tenantConfiguration.put("organizationId", thread.getOrganizationId().toString());
        }

        return ConversationContextDto.builder()
                .conversationId(thread.getId())
                .tenantId(thread.getTenantId())
                .channel(thread.getChannel())
                .status(thread.getStatus())
                .lead(lead)
                .customer(customer)
                .opportunity(opportunity)
                .catalogRecommendation(recommendation)
                .aiSummary(aiSummary)
                .previousMessages(messages)
                .knowledgeSnippets(new ArrayList<>())
                .tenantConfiguration(tenantConfiguration)
                .metadata(thread.getMetadata() == null ? new HashMap<>() : new HashMap<>(thread.getMetadata()))
                .build();
    }

    private LeadDto loadLead(UUID leadId) {
        if (leadId == null) {
            return null;
        }
        LeadServiceClient client = leadServiceClient.getIfAvailable();
        if (client == null) {
            return null;
        }
        try {
            ApiResponse<LeadDto> response = client.getLead(leadId);
            return response == null ? null : response.getData();
        } catch (Exception ex) {
            log.warn("Unable to load lead {} for conversation context: {}", leadId, ex.getMessage());
            return null;
        }
    }

    private CustomerDto loadCustomer(UUID customerId, UUID leadId) {
        CustomerServiceClient client = customerServiceClient.getIfAvailable();
        if (client == null) {
            return null;
        }
        try {
            if (customerId != null) {
                ApiResponse<CustomerDto> response = client.getCustomer(customerId);
                return response == null ? null : response.getData();
            }
            if (leadId != null) {
                ApiResponse<CustomerDto> response = client.getBySourceLead(leadId);
                return response == null ? null : response.getData();
            }
        } catch (Exception ex) {
            log.warn("Unable to load customer for conversation context: {}", ex.getMessage());
        }
        return null;
    }

    private OpportunityDto loadOpportunity(UUID opportunityId, UUID leadId) {
        DealServiceClient client = dealServiceClient.getIfAvailable();
        if (client == null) {
            return null;
        }
        try {
            if (opportunityId != null) {
                ApiResponse<OpportunityDto> response = client.getOpportunity(opportunityId);
                return response == null ? null : response.getData();
            }
            if (leadId != null) {
                ApiResponse<PageResponse<OpportunityDto>> response =
                        client.listOpportunities(0, 1, null, null, leadId);
                if (response != null
                        && response.getData() != null
                        && response.getData().getContent() != null
                        && !response.getData().getContent().isEmpty()) {
                    return response.getData().getContent().getFirst();
                }
            }
        } catch (Exception ex) {
            log.warn("Unable to load opportunity for conversation context: {}", ex.getMessage());
        }
        return null;
    }

    private CatalogRecommendationResultDto loadCatalogRecommendation(
            ConversationThread thread, LeadDto lead) {
        CatalogServiceClient client = catalogServiceClient.getIfAvailable();
        if (client == null || thread.getLeadId() == null) {
            return null;
        }
        try {
            CatalogMatchRequest match = CatalogMatchRequest.builder()
                    .leadId(thread.getLeadId())
                    .customerId(thread.getCustomerId())
                    .keyword(lead != null ? lead.getCustomerName() : null)
                    .limit(5)
                    .build();
            ApiResponse<CatalogRecommendationResultDto> response = client.recommend(
                    CatalogRecommendationRequest.builder()
                            .match(match)
                            .customerId(thread.getCustomerId())
                            .conversationId(thread.getId())
                            .build());
            return response == null ? null : response.getData();
        } catch (Exception ex) {
            log.warn("Unable to load catalog recommendation for conversation context: {}", ex.getMessage());
            return null;
        }
    }
}
