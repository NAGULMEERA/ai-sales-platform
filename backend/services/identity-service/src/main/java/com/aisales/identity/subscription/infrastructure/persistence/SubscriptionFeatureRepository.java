package com.aisales.identity.subscription.infrastructure.persistence;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import com.aisales.identity.subscription.domain.entity.SubscriptionFeature;
import com.aisales.identity.subscription.domain.enums.SubscriptionPlan;



public interface SubscriptionFeatureRepository extends JpaRepository<SubscriptionFeature, UUID> {

    Optional<SubscriptionFeature> findByPlanAndFeatureCode(SubscriptionPlan plan, String featureCode);
}
