package com.aisales.common.contracts.lead;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LeadDuplicateDto {
    private UUID id;
    private UUID leadId;
    private UUID duplicateOfLeadId;
    private BigDecimal similarityScore;
    private boolean resolved;
    private UUID mergedIntoLeadId;
    private Instant detectedAt;
}
