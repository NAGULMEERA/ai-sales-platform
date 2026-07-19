package com.aisales.common.contracts.customer;

import jakarta.validation.constraints.NotNull;
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
public class MergeCustomerRequest {

    /** Customer that will be absorbed and soft-deleted. */
    @NotNull
    private UUID loserCustomerId;

    @Size(max = 500)
    private String reason;
}
