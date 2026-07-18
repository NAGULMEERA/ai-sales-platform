package com.aisales.common.contracts.lead;

import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PipelineStageDto {

    private UUID id;
    private String stageCode;
    private String displayName;
    private int stageOrder;
    private boolean terminal;
}
