package com.aisales.common.contracts.lead;

import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConvertLeadRequest {

    /**
     * Optional. When omitted, lead-service creates (or reuses) a customer via customer-service
     * with {@code sourceType=LEAD_CONVERSION} and {@code sourceLeadId}.
     */
    private UUID customerId;

    private String reason;
}
