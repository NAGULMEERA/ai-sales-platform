package com.aisales.common.contracts.ai;

import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SentimentAnalysisDto {

    private UUID executionId;
    private String sentiment;
    private Double score;
    private String rationale;
}
