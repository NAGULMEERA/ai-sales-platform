package com.aisales.common.contracts.catalog;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateCatalogOfferRequest {

    @Size(max = 255)
    private String name;

    @Size(min = 3, max = 3)
    private String currency;

    @DecimalMin("0.0")
    private BigDecimal unitPrice;

    private CatalogItemStatus status;

    private Instant validFrom;

    private Instant validTo;
}
