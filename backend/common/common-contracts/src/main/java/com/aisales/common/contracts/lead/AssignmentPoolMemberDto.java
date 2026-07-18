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
public class AssignmentPoolMemberDto {
    private UUID id;
    private UUID tenantId;
    private UUID userId;
    private boolean enabled;
    private Instant lastAssignedAt;
}
