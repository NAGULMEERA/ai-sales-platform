package com.aisales.identity.application.service;

import com.aisales.identity.api.response.FeatureCheckResponse;
import com.aisales.identity.domain.entity.SubscriptionFeature;
import com.aisales.identity.domain.entity.TenantSubscription;
import com.aisales.identity.domain.enums.SubscriptionPlan;
import com.aisales.identity.infrastructure.persistence.SubscriptionFeatureRepository;
import com.aisales.identity.infrastructure.persistence.TenantSubscriptionRepository;
import com.aisales.common.exception.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FeatureCheckService {

    private final TenantSubscriptionRepository subscriptionRepository;
    private final SubscriptionFeatureRepository featureRepository;

    @Transactional(readOnly = true)
    public FeatureCheckResponse checkFeature(UUID tenantId, String featureCode) {
        TenantSubscription subscription = subscriptionRepository.findByTenantId(tenantId)
                .orElseThrow(() -> new NotFoundException("Subscription", tenantId));
        SubscriptionFeature feature = featureRepository
                .findByPlanAndFeatureCode(subscription.getPlan(), featureCode)
                .orElseThrow(() -> new NotFoundException("Feature", featureCode));

        return FeatureCheckResponse.builder()
                .featureCode(featureCode)
                .enabled(feature.isEnabled())
                .limitValue(feature.getLimitValue())
                .plan(subscription.getPlan().name())
                .build();
    }

    @Transactional(readOnly = true)
    public boolean isFeatureEnabled(UUID tenantId, String featureCode) {
        return subscriptionRepository.findByTenantId(tenantId)
                .flatMap(sub -> featureRepository.findByPlanAndFeatureCode(sub.getPlan(), featureCode))
                .map(SubscriptionFeature::isEnabled)
                .orElse(false);
    }
}
