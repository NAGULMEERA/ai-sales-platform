package com.aisales.common.contracts.deal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuoteLineItemRequest {

    /** Catalog product reference (validated via catalog-service when present). */
    private UUID productId;

    /** Catalog offer reference — preferred; snapshots price/code/name. */
    private UUID offerId;

    @NotNull
    @DecimalMin(value = "0.0001", inclusive = true)
    private BigDecimal quantity;
}
