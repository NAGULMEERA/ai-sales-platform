package com.aisales.identity.domain.entity;

import com.aisales.identity.domain.enums.SubscriptionPlan;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

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
