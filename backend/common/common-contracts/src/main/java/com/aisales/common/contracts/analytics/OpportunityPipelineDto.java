package com.aisales.common.contracts.analytics;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OpportunityPipelineDto {

    private long openCount;
    private long wonCount;
    private long lostCount;
    private BigDecimal openAmount;
    private BigDecimal wonAmount;
    private Double conversionRate;

    @Builder.Default
    private List<NamedCountDto> byStatus = new ArrayList<>();
}
