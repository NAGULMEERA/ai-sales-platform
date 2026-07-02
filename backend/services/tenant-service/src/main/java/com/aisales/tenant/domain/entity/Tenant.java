package com.aisales.tenant.domain.entity;

import com.aisales.common.contracts.tenant.SubscriptionPlan;
import com.aisales.common.contracts.tenant.TenantIndustry;
import com.aisales.common.contracts.tenant.TenantStatus;
import com.aisales.common.core.audit.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "tenants")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Tenant extends BaseEntity {

    @Column(name = "tenant_code", nullable = false, unique = true, length = 50)
    private String tenantCode;

    @Column(nullable = false, unique = true, length = 50)
    private String slug;

    @Column(nullable = false, length = 100)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private TenantIndustry industry;

    @Enumerated(EnumType.STRING)
    @Column(name = "subscription_plan", nullable = false, length = 50)
    private SubscriptionPlan subscriptionPlan;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TenantStatus status;

    @Column(nullable = false, length = 64)
    private String timezone;

    @Column(nullable = false, length = 10)
    private String language;

    @Column(name = "logo_url", length = 512)
    private String logoUrl;

    @Column(nullable = false)
    @Builder.Default
    private boolean deleted = false;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    @Column(name = "deleted_by")
    private String deletedBy;

    @Version
    @Column(nullable = false)
    @Builder.Default
    private Long version = 0L;
}
