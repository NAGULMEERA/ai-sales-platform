package com.aisales.common.contracts.ai;

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
public class LeadInsightsDto {

    private UUID executionId;
    private Integer intentScore;
    private Integer urgencyScore;
    private String buyerPersona;

    @Builder.Default
    private List<String> insights = new ArrayList<>();

    @Builder.Default
    private List<String> objections = new ArrayList<>();
}
