package com.aisales.common.contracts.lead;

import jakarta.validation.constraints.Size;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * AI-assisted qualification via AI Gateway. Same request shape for every industry;
 * {@code promptCode} and {@code variableKeys} default from enabled industry plugin config
 * when omitted (caller override still wins).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QualifyLeadWithAiRequest {

    /**
     * Prompt registry code (e.g. LEAD_QUALIFY_REAL_ESTATE). Optional when the tenant has an
     * enabled industry plugin with {@code qualificationPromptCode}.
     */
    @Size(max = 100)
    private String promptCode;

    /**
     * Attribute keys from lead.metadata to send as prompt variables.
     * Real Estate example: budget, location, timeline.
     * Automobile example: budget, vehicle, financeRequired, exchange.
     */
    @Builder.Default
    private List<String> variableKeys = new ArrayList<>();

    @Size(max = 2000)
    private String notes;
}
