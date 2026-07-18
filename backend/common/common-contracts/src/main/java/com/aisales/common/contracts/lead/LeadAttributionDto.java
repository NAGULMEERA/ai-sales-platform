package com.aisales.common.contracts.lead;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LeadAttributionDto {
    private UUID id;
    private UUID leadId;
    private String channel;
    private String campaign;
    private String adId;
    private Integer position;
    private BigDecimal cost;
    private Map<String, Object> sourceDetails;
    private Instant createdAt;
}
