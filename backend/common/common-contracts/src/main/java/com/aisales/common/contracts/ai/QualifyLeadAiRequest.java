package com.aisales.common.contracts.ai;

import jakarta.validation.constraints.Size;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * AI-side qualification orchestration request. Business services assemble context;
 * AI Gateway remains the only path to LLM providers.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QualifyLeadAiRequest {

    @Size(max = 100)
    private String promptCode;

    private UUID promptId;
    private Integer promptVersion;

    @Size(max = 64)
    private String industryCode;

    @Size(max = 16)
    private String languageCode;

    @Builder.Default
    @Size(max = 64)
    private String capability = "LEAD_QUALIFICATION";

    /** Lead / customer / conversation / catalog / workflow variables for prompt rendering. */
    @Builder.Default
    private Map<String, String> variables = new HashMap<>();

    private UUID knowledgeBaseId;

    @Size(max = 2000)
    private String retrievalQuery;

    private Integer retrievalTopK;

    @Size(max = 255)
    private String businessReference;

    private String leadId;
    private String customerId;
}
