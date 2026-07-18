package com.aisales.common.contracts.deal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateOpportunityRequest {

    @NotNull
    private UUID customerId;

    private UUID leadId;

    @NotBlank
    @Size(max = 255)
    private String name;

    @DecimalMin(value = "0.0", inclusive = true)
    private BigDecimal amount;

    @Size(max = 3)
    private String currency;

    @Min(0)
    @Max(100)
    private Integer probability;

    private LocalDate expectedCloseDate;

    /** Opportunity owner. Defaults from request context actor when omitted. */
    private UUID assignedTo;
}
