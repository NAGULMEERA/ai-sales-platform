package com.aisales.tenant.domain.service;

import com.aisales.common.contracts.tenant.SubscriptionPlan;
import com.aisales.common.contracts.tenant.TenantIndustry;
import com.aisales.common.contracts.tenant.TenantStatus;
import com.aisales.common.exception.exception.ValidationException;
import com.aisales.tenant.domain.entity.Tenant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TenantDomainServiceTest {

    private TenantDomainService domainService;

    @BeforeEach
    void setUp() {
        domainService = new TenantDomainService();
    }

    @Test
    void shouldRejectEmptySlug() {
        assertThatThrownBy(() -> domainService.validateSlug(""))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("slug cannot be empty");
    }

    @Test
    void shouldRejectInvalidSlugFormat() {
        assertThatThrownBy(() -> domainService.validateSlug("Invalid Slug"))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("lowercase");
    }

    @Test
    void shouldGenerateTenantCodeFromSlug() {
        assertThat(domainService.resolveTenantCode(null, "acme-corp")).isEqualTo("ACME_CORP");
    }

    @Test
    void shouldDefaultSubscriptionPlanToFree() {
        assertThat(domainService.resolveSubscriptionPlan(null)).isEqualTo(SubscriptionPlan.FREE);
    }

    @Test
    void shouldActivateActiveTenant() {
        Tenant tenant = activeTenant();
        domainService.activate(tenant);
        assertThat(tenant.getStatus()).isEqualTo(TenantStatus.ACTIVE);
    }

    @Test
    void shouldRejectActivateDeletedTenant() {
        Tenant tenant = activeTenant();
        tenant.setDeleted(true);
        assertThatThrownBy(() -> domainService.activate(tenant))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Deleted tenant");
    }

    @Test
    void shouldSoftDeleteTenant() {
        Tenant tenant = activeTenant();
        domainService.softDelete(tenant, "admin");
        assertThat(tenant.isDeleted()).isTrue();
        assertThat(tenant.getDeletedAt()).isNotNull();
        assertThat(tenant.getDeletedBy()).isEqualTo("admin");
        assertThat(tenant.getStatus()).isEqualTo(TenantStatus.SUSPENDED);
    }

    private Tenant activeTenant() {
        return Tenant.builder()
                .tenantCode("DEMO")
                .slug("demo")
                .name("Demo")
                .industry(TenantIndustry.REAL_ESTATE)
                .subscriptionPlan(SubscriptionPlan.FREE)
                .status(TenantStatus.ACTIVE)
                .timezone("UTC")
                .language("en")
                .deleted(false)
                .build();
    }
}
