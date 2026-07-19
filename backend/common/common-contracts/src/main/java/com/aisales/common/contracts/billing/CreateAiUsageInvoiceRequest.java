package com.aisales.common.contracts.billing;

import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateAiUsageInvoiceRequest {

    @NotNull
    private Instant periodFrom;

    @NotNull
    private Instant periodTo;

    /** When true, create as ISSUED; otherwise DRAFT. */
    @Builder.Default
    private boolean issue = false;
}
