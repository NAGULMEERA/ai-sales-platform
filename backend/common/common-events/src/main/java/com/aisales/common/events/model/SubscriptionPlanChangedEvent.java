package com.aisales.common.events.model;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * Integration event when a tenant subscription plan changes (e.g. FREE → PREMIUM).
 * AI Service applies plan-linked token quota packages; Billing remains separate.
 */
@Getter
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class SubscriptionPlanChangedEvent extends BaseEvent {

    private String previousPlan;
    private String newPlan;
    private String externalSubscriptionId;

    public static SubscriptionPlanChangedEvent of(
            String tenantId,
            String previousPlan,
            String newPlan,
            String externalSubscriptionId,
            String correlationId) {
        SubscriptionPlanChangedEvent event = new SubscriptionPlanChangedEvent();
        event.init("SubscriptionPlanChanged", tenantId, tenantId, correlationId);
        event.previousPlan = previousPlan;
        event.newPlan = newPlan;
        event.externalSubscriptionId = externalSubscriptionId;
        return event;
    }
}
