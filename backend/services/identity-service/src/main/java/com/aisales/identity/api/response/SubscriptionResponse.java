package com.aisales.identity.api.response;

import com.aisales.identity.domain.enums.SubscriptionPlan;
import com.aisales.identity.domain.enums.SubscriptionStatus;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
public class SubscriptionResponse {

    private UUID tenantId;
    private SubscriptionPlan plan;
    private SubscriptionStatus status;
    private Instant currentPeriodEnd;
}
