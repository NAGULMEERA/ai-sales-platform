package com.aisales.common.contracts.billing;

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
public class InvoiceLineItemDto {

    private UUID id;
    private String lineCode;
    private String description;
    private long quantity;
    private BigDecimal unitAmountUsd;
    private BigDecimal lineTotalUsd;
}
