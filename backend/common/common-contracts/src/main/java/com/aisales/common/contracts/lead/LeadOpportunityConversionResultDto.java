package com.aisales.common.contracts.lead;

import com.aisales.common.contracts.catalog.CatalogRecommendationResultDto;
import com.aisales.common.contracts.deal.OpportunityDto;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LeadOpportunityConversionResultDto {

    private LeadDto lead;
    private UUID customerId;
    private OpportunityDto opportunity;
    private CatalogRecommendationResultDto recommendation;
    private UUID selectedProductId;
    private UUID selectedOfferId;
}
