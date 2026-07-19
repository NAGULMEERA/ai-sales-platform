package com.aisales.common.contracts.deal;

import java.math.BigDecimal;
import java.time.Instant;
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
public class OpportunityDto {

    private UUID id;
    private UUID tenantId;
    private UUID organizationId;
    private UUID leadId;
    private UUID customerId;
    private String name;
    private BigDecimal amount;
    private String currency;
    private OpportunityStatus status;
    private Integer probability;
    private Integer score;
    private LocalDate expectedCloseDate;
    private UUID assignedTo;
    private UUID catalogProductId;
    private UUID catalogOfferId;
    private String notes;
    private String closeReason;
    private Instant createdAt;
    private Instant updatedAt;
    private Long version;
}
