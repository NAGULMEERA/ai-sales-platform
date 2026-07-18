package com.aisales.common.contracts.lead;

import jakarta.validation.constraints.Size;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssignLeadRequest {

    /**
     * Defaults to MANUAL when null. MANUAL requires {@link #assignedTo};
     * ROUND_ROBIN picks the least-recently-assigned enabled pool member.
     */
    private AssignmentStrategy strategy;

    private UUID assignedTo;

    @Size(max = 255)
    private String assignmentReason;
}
