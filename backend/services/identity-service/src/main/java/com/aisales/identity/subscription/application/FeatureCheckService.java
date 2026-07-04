package com.aisales.identity.subscription.application;

import com.aisales.common.exception.exception.NotFoundException;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.aisales.identity.subscription.api.response.FeatureCheckResponse;
import com.aisales.identity.subscription.domain.entity.SubscriptionFeature;
import com.aisales.identity.subscription.domain.entity.TenantSubscription;
import com.aisales.identity.subscription.infrastructure.persistence.SubscriptionFeatureRepository;
import com.aisales.identity.subscription.infrastructure.persistence.TenantSubscriptionRepository;



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
