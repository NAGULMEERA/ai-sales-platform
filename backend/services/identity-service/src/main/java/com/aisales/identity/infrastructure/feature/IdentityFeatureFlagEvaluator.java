package com.aisales.identity.infrastructure.feature;

import com.aisales.common.contracts.platform.FeatureFlagEvaluator;
import com.aisales.identity.application.service.FeatureCheckService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class IdentityFeatureFlagEvaluator implements FeatureFlagEvaluator {

    private final FeatureCheckService featureCheckService;

    @Override
    public boolean isEnabled(String tenantId, String featureKey) {
        return featureCheckService.isFeatureEnabled(UUID.fromString(tenantId), featureKey);
    }

    @Override
    public boolean isEnabledWithRollout(String tenantId, String featureKey) {
        return isEnabled(tenantId, featureKey);
    }
}
