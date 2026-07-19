package com.aisales.common.contracts.analytics;

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
public class LeadFunnelDto {

    private long created;
    private long validated;
    private long qualified;
    private long contacted;
    private long converted;
    private long lost;
    private Double conversionRate;

    @Builder.Default
    private List<FunnelStageDto> stages = new ArrayList<>();
}
