package com.aisales.common.contracts.lead;

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
public class LeadStatusHistoryDto {
    private UUID id;
    private UUID leadId;
    private LeadStatus oldStatus;
    private LeadStatus newStatus;
    private String reason;
    private UUID changedBy;
    private Instant createdAt;
}
