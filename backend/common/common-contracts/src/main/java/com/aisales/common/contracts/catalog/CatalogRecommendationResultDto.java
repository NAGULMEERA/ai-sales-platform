package com.aisales.common.contracts.catalog;

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
public class CatalogRecommendationResultDto {

    private UUID leadId;
    private UUID customerId;

    @Builder.Default
    private List<CatalogMatchCandidateDto> recommendations = new ArrayList<>();

    @Builder.Default
    private List<CatalogMatchCandidateDto> alternatives = new ArrayList<>();

    private String rationale;
    private Double overallConfidence;
}
