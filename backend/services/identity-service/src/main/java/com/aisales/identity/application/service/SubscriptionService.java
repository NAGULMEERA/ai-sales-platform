package com.aisales.identity.application.service;

import com.aisales.identity.api.response.SubscriptionResponse;
import com.aisales.identity.domain.entity.TenantSubscription;
import com.aisales.identity.domain.enums.SubscriptionPlan;
import com.aisales.identity.domain.enums.SubscriptionStatus;
import com.aisales.common.exception.exception.NotFoundException;
import com.aisales.identity.infrastructure.persistence.TenantSubscriptionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SubscriptionService {

    private final TenantSubscriptionRepository subscriptionRepository;
    private final AuditService auditService;

    @Transactional(readOnly = true)
    public SubscriptionResponse getSubscription(UUID tenantId) {
        return toResponse(subscriptionRepository.findByTenantId(tenantId)
                .orElseThrow(() -> new NotFoundException("Subscription", tenantId)));
    }

    @Transactional
    public SubscriptionResponse upgradeToPremium(UUID tenantId, UUID userId, String externalSubscriptionId) {
        TenantSubscription subscription = subscriptionRepository.findByTenantId(tenantId)
                .orElseThrow(() -> new NotFoundException("Subscription", tenantId));
        subscription.setPlan(SubscriptionPlan.PREMIUM);
        subscription.setStatus(SubscriptionStatus.ACTIVE);
        subscription.setPaymentProvider("stripe");
        subscription.setExternalSubscriptionId(externalSubscriptionId);
        subscription.setCurrentPeriodEnd(Instant.now().plusSeconds(30L * 24 * 3600));
        subscription.setUpdatedAt(Instant.now());
        subscription = subscriptionRepository.save(subscription);
        auditService.log(tenantId, userId, "SUBSCRIPTION_UPGRADED", "subscription", subscription.getId().toString(),
                null, null, "{\"plan\":\"PREMIUM\"}");
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
