package com.aisales.common.contracts.lead;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
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
public class CreateLeadFollowupRequest {

    @NotNull
    private Instant scheduledAt;

    @NotBlank
    @Size(max = 50)
    private String followupType;

    @Size(max = 2000)
    private String note;

    private UUID assignedTo;
}
