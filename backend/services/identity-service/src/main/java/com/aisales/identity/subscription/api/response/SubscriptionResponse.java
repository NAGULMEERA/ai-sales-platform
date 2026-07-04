package com.aisales.identity.subscription.api.response;

import java.time.Instant;
import java.util.UUID;
import lombok.Builder;
import lombok.Data;
import com.aisales.identity.subscription.domain.enums.SubscriptionPlan;
import com.aisales.identity.subscription.domain.enums.SubscriptionStatus;



@Data
@Builder
public class SubscriptionResponse {

    private UUID tenantId;
    private SubscriptionPlan plan;
    private SubscriptionStatus status;
    private Instant currentPeriodEnd;
}
