package com.aisales.lead.application.service;

import com.aisales.common.contracts.catalog.CatalogMatchCandidateDto;
import com.aisales.common.contracts.catalog.CatalogMatchRequest;
import com.aisales.common.contracts.catalog.CatalogRecommendationRequest;
import com.aisales.common.contracts.catalog.CatalogRecommendationResultDto;
import com.aisales.common.contracts.client.CatalogServiceClient;
import com.aisales.common.contracts.client.DealServiceClient;
import com.aisales.common.contracts.deal.CreateOpportunityRequest;
import com.aisales.common.contracts.deal.OpportunityDto;
import com.aisales.common.contracts.deal.OpportunityStatus;
import com.aisales.common.contracts.deal.ScoreOpportunityRequest;
import com.aisales.common.contracts.deal.UpdateOpportunityStageRequest;
import com.aisales.common.contracts.lead.ConvertLeadRequest;
import com.aisales.common.contracts.lead.ConvertLeadToOpportunityRequest;
import com.aisales.common.contracts.lead.LeadOpportunityConversionResultDto;
import com.aisales.common.contracts.lead.LeadStatus;
import com.aisales.common.core.dto.ApiResponse;
import com.aisales.common.core.util.TenantContext;
import com.aisales.common.exception.exception.NotFoundException;
import com.aisales.common.exception.exception.ValidationException;
import com.aisales.lead.application.mapper.LeadMapper;
import com.aisales.lead.domain.entity.Lead;
import com.aisales.lead.infrastructure.persistence.LeadRepository;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * Orchestrates Qualified Lead → Catalog Match → Recommendation → Opportunity.
 * Business decisions remain in owning services; this coordinates via Feign clients.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LeadOpportunityConversionService {

    private final LeadRepository leadRepository;
    private final LeadService leadService;
    private final LeadCustomerConversionGateway customerConversionGateway;
    private final CatalogServiceClient catalogServiceClient;
    private final DealServiceClient dealServiceClient;
    private final LeadSideEffectRecorder sideEffects;
    private final LeadMapper leadMapper;

    public LeadOpportunityConversionResultDto convertToOpportunity(
            UUID leadId, ConvertLeadToOpportunityRequest request) {
        Lead lead = leadRepository.findByTenantIdAndIdAndDeletedAtIsNull(requireTenantId(), leadId)
                .orElseThrow(() -> new NotFoundException("Lead not found: " + leadId));

        if (lead.getStatus() != LeadStatus.QUALIFIED && lead.getStatus() != LeadStatus.WON) {
            throw new ValidationException(
                    "Lead must be QUALIFIED before opportunity conversion; current=" + lead.getStatus());
        }

        ConvertLeadToOpportunityRequest body = request != null
                ? request
                : ConvertLeadToOpportunityRequest.builder().build();

        UUID customerId = resolveCustomerId(lead, body.getCustomerId());
        if (lead.getCustomerId() == null || !customerId.equals(lead.getCustomerId())) {
            leadService.convertLead(leadId, ConvertLeadRequest.builder()
                    .customerId(customerId)
                    .reason("Opportunity conversion")
                    .build());
            lead = leadRepository.findByTenantIdAndIdAndDeletedAtIsNull(requireTenantId(), leadId)
                    .orElse(lead);
        }

        CatalogRecommendationResultDto recommendation = recommend(lead, body);
        CatalogMatchCandidateDto selected = selectTop(recommendation);

        if (selected == null && !body.isAllowWithoutMatch()) {
            throw new ValidationException("No catalog match found for lead conversion");
        }

        CreateOpportunityRequest createRequest = CreateOpportunityRequest.builder()
                .customerId(customerId)
                .leadId(leadId)
                .name(resolveOpportunityName(lead, body, selected))
                .amount(selected != null ? selected.getUnitPrice() : null)
                .currency(selected != null && StringUtils.hasText(selected.getCurrency())
                        ? selected.getCurrency()
                        : (StringUtils.hasText(body.getCurrency()) ? body.getCurrency() : "INR"))
                .probability(lead.getScore() != null ? Math.min(lead.getScore(), 90) : 50)
                .assignedTo(lead.getAssignedTo())
                .catalogProductId(selected != null ? selected.getProductId() : null)
                .catalogOfferId(selected != null ? selected.getOfferId() : null)
                .notes(body.getNotes())
                .build();

        ApiResponse<OpportunityDto> opportunityResponse = dealServiceClient.createOpportunity(createRequest);
        OpportunityDto opportunity = opportunityResponse != null ? opportunityResponse.getData() : null;
        if (opportunity == null) {
            throw new ValidationException("Deal service returned empty opportunity");
        }

        opportunity = qualifyOpportunity(opportunity, lead, selected);
        scoreOpportunity(opportunity, lead, selected);

        UUID actor = parseUuidOrNull(TenantContext.getUserId());
        sideEffects.recordActivity(
                leadId,
                "OPPORTUNITY_CREATED",
                "Opportunity " + opportunity.getId() + " created from catalog recommendation",
                actor);
        if (selected != null) {
            sideEffects.recordActivity(
                    leadId,
                    "CATALOG_MATCHED",
                    "Matched catalog product " + selected.getProductCode(),
                    actor);
            sideEffects.recordActivity(
                    leadId,
                    "RECOMMENDATION_GENERATED",
                    "Catalog recommendation confidence "
                            + (recommendation.getOverallConfidence() != null
                                    ? recommendation.getOverallConfidence()
                                    : selected.getConfidenceScore() / 100.0),
                    actor);
        }

        return LeadOpportunityConversionResultDto.builder()
                .lead(leadMapper.toDto(lead))
                .customerId(customerId)
                .opportunity(opportunity)
                .recommendation(recommendation)
                .selectedProductId(selected != null ? selected.getProductId() : null)
                .selectedOfferId(selected != null ? selected.getOfferId() : null)
                .build();
    }

    private CatalogRecommendationResultDto recommend(Lead lead, ConvertLeadToOpportunityRequest body) {
        Map<String, Object> filters = new HashMap<>();
        if (body.getAttributeFilters() != null) {
            filters.putAll(body.getAttributeFilters());
        }
        Map<String, Object> leadAttrs = lead.getAttributes() != null ? lead.getAttributes() : Map.of();
        putIfAbsent(filters, "location", leadAttrs.get("location"));
        putIfAbsent(filters, "bedrooms", leadAttrs.get("bedrooms"));
        putIfAbsent(filters, "make", leadAttrs.get("make"));
        putIfAbsent(filters, "model", leadAttrs.get("model"));

        BigDecimal maxPrice = body.getMaxPrice();
        if (maxPrice == null && leadAttrs.get("budget") != null) {
            try {
                maxPrice = new BigDecimal(String.valueOf(leadAttrs.get("budget")));
            } catch (NumberFormatException ignored) {
                // leave null
            }
        }

        CatalogMatchRequest match = CatalogMatchRequest.builder()
                .leadId(lead.getId())
                .customerId(lead.getCustomerId())
                .category(body.getCategory())
                .productType(body.getProductType())
                .maxPrice(maxPrice)
                .budget(maxPrice)
                .currency(body.getCurrency())
                .location(filters.get("location") != null ? String.valueOf(filters.get("location")) : null)
                .attributeFilters(filters)
                .preferences(new HashMap<>(leadAttrs))
                .scoringWeights(body.getScoringWeights())
                .limit(body.getMatchLimit() != null ? body.getMatchLimit() : 5)
                .build();

        ApiResponse<CatalogRecommendationResultDto> response = catalogServiceClient.recommend(
                CatalogRecommendationRequest.builder()
                        .match(match)
                        .customerId(lead.getCustomerId())
                        .customerPreferences(new HashMap<>(leadAttrs))
                        .scoringWeights(body.getScoringWeights())
                        .includeAlternatives(true)
                        .build());
        CatalogRecommendationResultDto data = response != null ? response.getData() : null;
        if (data == null) {
            return CatalogRecommendationResultDto.builder()
                    .leadId(lead.getId())
                    .recommendations(List.of())
                    .alternatives(List.of())
                    .build();
        }
        return data;
    }

    private OpportunityDto qualifyOpportunity(
            OpportunityDto opportunity, Lead lead, CatalogMatchCandidateDto selected) {
        try {
            ApiResponse<OpportunityDto> response = dealServiceClient.updateOpportunityStage(
                    opportunity.getId(),
                    UpdateOpportunityStageRequest.builder()
                            .status(OpportunityStatus.QUALIFIED)
                            .reason("Lead " + lead.getId() + " converted with catalog match")
                            .build());
            if (response != null && response.getData() != null) {
                return response.getData();
            }
        } catch (RuntimeException ex) {
            log.warn("Could not advance opportunity {} to QUALIFIED: {}", opportunity.getId(), ex.getMessage());
        }
        return opportunity;
    }

    private void scoreOpportunity(
            OpportunityDto opportunity, Lead lead, CatalogMatchCandidateDto selected) {
        try {
            dealServiceClient.scoreOpportunity(
                    opportunity.getId(),
                    ScoreOpportunityRequest.builder()
                            .leadScore(lead.getScore())
                            .catalogMatchScore(selected != null ? selected.getMatchScore() : null)
                            .aiConfidenceScore(selected != null ? selected.getConfidenceScore() : null)
                            .pipelineStageScore(70)
                            .build());
        } catch (RuntimeException ex) {
            log.warn("Could not score opportunity {}: {}", opportunity.getId(), ex.getMessage());
        }
    }

    private UUID resolveCustomerId(Lead lead, UUID requestedCustomerId) {
        if (requestedCustomerId != null) {
            return requestedCustomerId;
        }
        if (lead.getCustomerId() != null) {
            return lead.getCustomerId();
        }
        return customerConversionGateway.resolveCustomerId(lead, null);
    }

    private static CatalogMatchCandidateDto selectTop(CatalogRecommendationResultDto recommendation) {
        if (recommendation == null || recommendation.getRecommendations() == null
                || recommendation.getRecommendations().isEmpty()) {
            return null;
        }
        return recommendation.getRecommendations().getFirst();
    }

    private static String resolveOpportunityName(
            Lead lead, ConvertLeadToOpportunityRequest body, CatalogMatchCandidateDto selected) {
        if (StringUtils.hasText(body.getOpportunityName())) {
            return body.getOpportunityName().trim();
        }
        if (selected != null && StringUtils.hasText(selected.getProductName())) {
            return selected.getProductName() + " — " + lead.getCustomerName();
        }
        return "Opportunity for " + lead.getCustomerName();
    }

    private static void putIfAbsent(Map<String, Object> map, String key, Object value) {
        if (value != null && !map.containsKey(key)) {
            map.put(key, value);
        }
    }

    private UUID requireTenantId() {
        String tenant = TenantContext.getTenantId();
        if (!StringUtils.hasText(tenant)) {
            throw new ValidationException("Tenant context is required");
        }
        return UUID.fromString(tenant);
    }

    private static UUID parseUuidOrNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }
}
