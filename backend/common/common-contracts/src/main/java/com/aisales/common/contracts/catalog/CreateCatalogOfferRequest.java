package com.aisales.common.contracts.catalog;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
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
public class CreateCatalogOfferRequest {

    @NotNull
    private UUID productId;

    @NotBlank
    @Size(max = 64)
    private String code;

    @NotBlank
    @Size(max = 255)
    private String name;

    @Size(min = 3, max = 3)
    private String currency;

    @NotNull
    @DecimalMin("0.0")
    private BigDecimal unitPrice;

    private CatalogItemStatus status;

    private Instant validFrom;

    private Instant validTo;
}
