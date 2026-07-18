package com.aisales.common.contracts.lead;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PipelineTransitionDto {

    private String fromStage;
    private String toStage;
}
