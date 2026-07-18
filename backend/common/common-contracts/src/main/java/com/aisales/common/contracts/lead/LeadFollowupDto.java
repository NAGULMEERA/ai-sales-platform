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
public class LeadFollowupDto {
    private UUID id;
    private UUID leadId;
    private Instant scheduledAt;
    private Instant completedAt;
    private String followupType;
    private String note;
    private UUID assignedTo;
    private UUID createdBy;
    private Instant createdAt;
}
