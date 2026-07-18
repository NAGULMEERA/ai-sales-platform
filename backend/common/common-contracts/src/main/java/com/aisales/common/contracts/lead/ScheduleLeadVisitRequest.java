package com.aisales.common.contracts.lead;

import jakarta.validation.constraints.Size;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScheduleLeadVisitRequest {

    /** Optional planned visit time (orchestration may live in appointment-service). */
    private Instant scheduledAt;

    @Size(max = 500)
    private String location;

    @Size(max = 1000)
    private String notes;
}
