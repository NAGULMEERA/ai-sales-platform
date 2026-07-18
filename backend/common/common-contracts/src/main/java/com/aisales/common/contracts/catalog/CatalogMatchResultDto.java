package com.aisales.common.contracts.catalog;

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
public class CatalogMatchResultDto {

    private UUID leadId;
    private List<CatalogMatchCandidateDto> candidates;
}
