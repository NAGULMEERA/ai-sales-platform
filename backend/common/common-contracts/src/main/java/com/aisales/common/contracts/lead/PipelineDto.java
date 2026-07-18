package com.aisales.common.contracts.lead;

import java.time.Instant;
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
public class PipelineDto {

    private UUID id;
    private UUID tenantId;
    private String code;
    private String name;
    private String description;
    private boolean active;
    private boolean defaultPipeline;
    private List<PipelineStageDto> stages;
    private List<PipelineTransitionDto> transitions;
    private Instant createdAt;
    private Instant updatedAt;
}
