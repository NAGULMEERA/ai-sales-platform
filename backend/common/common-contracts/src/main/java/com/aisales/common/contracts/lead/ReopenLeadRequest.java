package com.aisales.common.contracts.lead;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReopenLeadRequest {

    /** Target status after reopen; defaults to QUALIFIED. */
    private LeadStatus status;

    private String reason;
}
