package com.aisales.common.contracts.conversation;

import com.aisales.common.contracts.ai.ConversationSummaryDto;
import com.aisales.common.contracts.catalog.CatalogRecommendationResultDto;
import com.aisales.common.contracts.customer.CustomerDto;
import com.aisales.common.contracts.deal.OpportunityDto;
import com.aisales.common.contracts.lead.LeadDto;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Assembled business + AI context for a conversation, forwarded to AI Gateway.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConversationContextDto {

    private UUID conversationId;
    private UUID tenantId;
    private ConversationChannel channel;
    private ConversationStatus status;
    private LeadDto lead;
    private CustomerDto customer;
    private OpportunityDto opportunity;
    private CatalogRecommendationResultDto catalogRecommendation;
    private ConversationSummaryDto aiSummary;

    @Builder.Default
    private List<ConversationMessageDto> previousMessages = new ArrayList<>();

    @Builder.Default
    private List<String> knowledgeSnippets = new ArrayList<>();

    @Builder.Default
    private Map<String, Object> tenantConfiguration = new HashMap<>();

    @Builder.Default
    private Map<String, Object> metadata = new HashMap<>();
}
