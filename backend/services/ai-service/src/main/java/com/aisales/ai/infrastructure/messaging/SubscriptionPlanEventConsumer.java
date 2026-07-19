package com.aisales.ai.infrastructure.messaging;

import com.aisales.ai.application.service.AiQuotaService;
import com.aisales.common.events.consumer.IntegrationEventListener;
import com.aisales.common.events.model.SubscriptionPlanChangedEvent;
import com.aisales.common.events.model.TenantCreatedEvent;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Applies plan-linked AI quota packages when tenants are created or subscription plans change.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "aisales.events.inbox.enabled", havingValue = "true")
public class SubscriptionPlanEventConsumer {

    static final String TENANT_CREATED_CONSUMER = "ai-quota-tenant-created";
    static final String PLAN_CHANGED_CONSUMER = "ai-quota-subscription-plan-changed";

    private final IntegrationEventListener integrationEventListener;
    private final AiQuotaService aiQuotaService;

    @KafkaListener(
            topics = "${aisales.events.default-topic:aisales-events}",
            groupId = "ai-service-quota-plans",
            containerFactory = "integrationKafkaListenerContainerFactory")
    public void onMessage(ConsumerRecord<String, String> record) {
        integrationEventListener.handleIfType(
                record,
                TENANT_CREATED_CONSUMER,
                TenantCreatedEvent.EVENT_TYPE, TenantCreatedEvent.class,
                this::onTenantCreated);
        integrationEventListener.handleIfType(
                record,
                PLAN_CHANGED_CONSUMER,
                SubscriptionPlanChangedEvent.EVENT_TYPE, SubscriptionPlanChangedEvent.class,
                this::onPlanChanged);
    }

    void onTenantCreated(TenantCreatedEvent event) {
        applyPlan(event.getTenantId(), event.getPlan());
    }

    void onPlanChanged(SubscriptionPlanChangedEvent event) {
        applyPlan(event.getTenantId(), event.getNewPlan());
    }

    private void applyPlan(String tenantIdRaw, String plan) {
        if (!StringUtils.hasText(tenantIdRaw) || !StringUtils.hasText(plan)) {
            log.warn("Skipping AI quota plan apply: missing tenantId or plan");
            return;
        }
        UUID tenantId = UUID.fromString(tenantIdRaw);
        aiQuotaService.applyPlanPackage(tenantId, plan);
        log.info("Applied AI quota plan package tenant_id={} plan={}", tenantId, plan);
    }
}
