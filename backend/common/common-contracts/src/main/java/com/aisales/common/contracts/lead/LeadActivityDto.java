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
public class LeadActivityDto {
    private UUID id;
    private UUID leadId;
    private String activityType;
    private String description;
    private UUID createdBy;
    private Instant createdAt;
}
