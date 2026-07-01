package com.aisales.common.contracts.platform;

/**
 * Evaluates tenant-scoped feature flags. Implemented by tenant-service; consumed by business services.
 */
public interface FeatureFlagEvaluator {

    boolean isEnabled(String tenantId, String featureKey);

    boolean isEnabledWithRollout(String tenantId, String featureKey);
}
