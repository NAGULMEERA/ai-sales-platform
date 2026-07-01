package com.aisales.identity.infrastructure.persistence;

import com.aisales.identity.domain.entity.SubscriptionFeature;
import com.aisales.identity.domain.enums.SubscriptionPlan;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface SubscriptionFeatureRepository extends JpaRepository<SubscriptionFeature, UUID> {

    Optional<SubscriptionFeature> findByPlanAndFeatureCode(SubscriptionPlan plan, String featureCode);
}
