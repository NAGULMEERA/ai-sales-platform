package com.aisales.common.contracts.deal;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
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
public class CreateQuoteRequest {

    @NotNull
    private UUID opportunityId;

    @Size(max = 3)
    private String currency;

    private LocalDate validUntil;

    @Size(max = 2000)
    private String notes;

    @NotEmpty
    @Valid
    @Builder.Default
    private List<QuoteLineItemRequest> lineItems = new ArrayList<>();
}
