package com.aisales.common.contracts.deal;

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
public class QuoteLineItemDto {

    private UUID id;
    private UUID productId;
    private UUID offerId;
    private String code;
    private String name;
    private BigDecimal quantity;
    private BigDecimal unitPrice;
    private String currency;
    private BigDecimal lineTotal;
}
