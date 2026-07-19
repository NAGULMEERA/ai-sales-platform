package com.aisales.common.contracts.customer;

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
public class CustomerConsentDto {

    private UUID id;
    private UUID customerId;
    private String consentType;
    private String consentVersion;
    private boolean granted;
    private Instant grantedAt;
    private Instant withdrawnAt;
    private String source;
}
