package com.aisales.common.contracts.lead;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpsertAssignmentPoolMemberRequest {

    @NotNull
    private UUID userId;

    private Boolean enabled;
}
