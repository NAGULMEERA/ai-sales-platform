package com.aisales.common.contracts.customer;

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
public class CustomerDuplicateDto {

    private UUID id;
    private UUID customerId;
    private UUID duplicateOfCustomerId;
    private String matchReasons;
    private BigDecimal similarityScore;
    private String matchStrength;
    private Instant detectedAt;
    private boolean resolved;
    private UUID mergedIntoCustomerId;
}
