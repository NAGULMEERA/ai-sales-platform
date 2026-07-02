package com.aisales.tenant.application.audit;

import com.aisales.common.core.audit.Auditable;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class TenantLifecycleAuditor {

    @Auditable(action = "TENANT_CREATED", resourceType = "TENANT")
    public void tenantCreated(UUID tenantId) {
    }

    @Auditable(action = "TENANT_UPDATED", resourceType = "TENANT")
    public void tenantUpdated(UUID tenantId) {
    }

    @Auditable(action = "TENANT_ACTIVATED", resourceType = "TENANT")
    public void tenantActivated(UUID tenantId) {
    }

    @Auditable(action = "TENANT_SUSPENDED", resourceType = "TENANT")
    public void tenantSuspended(UUID tenantId) {
    }

    @Auditable(action = "TENANT_DELETED", resourceType = "TENANT")
    public void tenantDeleted(UUID tenantId) {
    }
}
