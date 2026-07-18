package com.aisales.common.contracts.ai;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiExecuteResponse {

    private UUID executionId;
    private UUID promptId;
    private String promptCode;
    private Integer promptVersion;
    private String provider;
    private String model;
    private String renderedSystemPrompt;
    private String renderedUserPrompt;
    private String rawText;
    @Builder.Default
    private Map<String, Object> structuredOutput = new HashMap<>();
    private Double confidence;
    private Integer promptTokens;
    private Integer completionTokens;
    private String businessReference;
}
