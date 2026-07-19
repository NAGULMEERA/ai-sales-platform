package com.aisales.common.contracts.catalog;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CatalogMatchCandidateDto {

    private UUID productId;
    private String productCode;
    private String productName;
    private CatalogProductType productType;
    private String category;
    private UUID offerId;
    private String offerCode;
    private String currency;
    private BigDecimal unitPrice;
    /** 0–100 hybrid ranking score. */
    private int matchScore;
    /** 0–100 confidence in the recommendation. */
    private int confidenceScore;
    private CatalogMatchScoreFactorsDto scoreFactors;
    private String reason;

    @Builder.Default
    private List<String> reasons = new ArrayList<>();
}
