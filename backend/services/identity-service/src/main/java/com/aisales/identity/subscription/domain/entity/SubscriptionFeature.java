package com.aisales.identity.subscription.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import com.aisales.identity.subscription.domain.enums.SubscriptionPlan;



@Entity
@Table(name = "subscription_features")
@Getter
@Setter
public class SubscriptionFeature {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SubscriptionPlan plan;

    @Column(name = "feature_code", nullable = false)
    private String featureCode;

    @Column(nullable = false)
    private boolean enabled;

    @Column(name = "limit_value")
    private Long limitValue;
}
