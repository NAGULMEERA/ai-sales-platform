package com.aisales.common.contracts.analytics;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FunnelStageDto {

    private String stage;
    private long count;
    private Double conversionFromPrevious;
}
