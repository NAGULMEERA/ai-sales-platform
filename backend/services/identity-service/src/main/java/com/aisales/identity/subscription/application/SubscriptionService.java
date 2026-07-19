package com.aisales.identity.subscription.application;

import com.aisales.common.core.util.CorrelationIdUtils;
import com.aisales.common.events.model.SubscriptionPlanChangedEvent;
import com.aisales.common.events.publisher.EventPublisher;
import com.aisales.common.exception.exception.NotFoundException;
import com.aisales.identity.audit.application.AuditService;
import com.aisales.identity.audit.domain.AuditAction;
import com.aisales.identity.audit.domain.AuditDetails;
import com.aisales.identity.subscription.api.response.SubscriptionResponse;
import com.aisales.identity.subscription.domain.entity.TenantSubscription;
import com.aisales.identity.subscription.domain.enums.SubscriptionPlan;
import com.aisales.identity.subscription.domain.enums.SubscriptionStatus;
import com.aisales.identity.subscription.infrastructure.persistence.TenantSubscriptionRepository;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SubscriptionService {

    private final TenantSubscriptionRepository subscriptionRepository;
    private final AuditService auditService;
    private final EventPublisher eventPublisher;

    @Transactional(readOnly = true)
    public SubscriptionResponse getSubscription(UUID tenantId) {
        return toResponse(subscriptionRepository.findByTenantId(tenantId)
                .orElseThrow(() -> new NotFoundException("Subscription", tenantId)));
    }

    @Transactional
    public SubscriptionResponse upgradeToPremium(UUID tenantId, UUID userId, String externalSubscriptionId) {
        TenantSubscription subscription = subscriptionRepository.findByTenantId(tenantId)
                .orElseThrow(() -> new NotFoundException("Subscription", tenantId));
        SubscriptionPlan previous = subscription.getPlan();
        subscription.setPlan(SubscriptionPlan.PREMIUM);
        subscription.setStatus(SubscriptionStatus.ACTIVE);
        subscription.setPaymentProvider("stripe");
        subscription.setExternalSubscriptionId(externalSubscriptionId);
        subscription.setCurrentPeriodEnd(Instant.now().plusSeconds(30L * 24 * 3600));
        subscription.setUpdatedAt(Instant.now());
        subscription = subscriptionRepository.save(subscription);
        auditService.logSecurityEvent(tenantId, userId, AuditAction.SUBSCRIPTION_UPGRADED, "subscription",
                subscription.getId().toString(), null, null, AuditDetails.plan(SubscriptionPlan.PREMIUM.name()));
        eventPublisher.publish(SubscriptionPlanChangedEvent.of(
                tenantId.toString(),
                previous != null ? previous.name() : null,
                SubscriptionPlan.PREMIUM.name(),
                externalSubscriptionId,
                CorrelationIdUtils.get().orElseGet(CorrelationIdUtils::generate)));
        return toResponse(subscription);
    }

    private SubscriptionResponse toResponse(TenantSubscription subscription) {
        return SubscriptionResponse.builder()
                .tenantId(subscription.getTenantId())
                .plan(subscription.getPlan())
                .status(subscription.getStatus())
                .currentPeriodEnd(subscription.getCurrentPeriodEnd())
                .build();
    }
}
