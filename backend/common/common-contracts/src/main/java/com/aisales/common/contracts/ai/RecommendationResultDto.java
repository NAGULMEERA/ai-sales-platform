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
public class RecommendationResultDto {

    private UUID executionId;
    private String recommendationType;
    private String title;
    private String rationale;
    private Double confidence;

    @Builder.Default
    private List<String> actions = new ArrayList<>();
}
