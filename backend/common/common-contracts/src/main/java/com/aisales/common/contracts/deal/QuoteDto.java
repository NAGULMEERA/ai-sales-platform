package com.aisales.common.contracts.deal;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuoteDto {

    private UUID id;
    private UUID tenantId;
    private UUID opportunityId;
    private Integer quoteVersion;
    private QuoteStatus status;
    private String currency;
    private BigDecimal totalAmount;
    private LocalDate validUntil;
    private String notes;
    @Builder.Default
    private List<QuoteLineItemDto> lineItems = new ArrayList<>();
    private Instant createdAt;
    private Instant updatedAt;
    private Long version;
}
