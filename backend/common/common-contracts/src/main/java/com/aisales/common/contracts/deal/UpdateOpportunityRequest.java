package com.aisales.common.contracts.deal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateOpportunityRequest {

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

    private OpportunityStatus status;
}
