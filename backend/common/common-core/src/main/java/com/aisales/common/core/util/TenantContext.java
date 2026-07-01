package com.aisales.common.core.util;

import com.aisales.common.core.tenant.TenantIsolationMode;

import java.util.Optional;
import java.util.UUID;

/**
 * Request-scoped tenant context. Populated from JWT; cleared after each request.
 */
public final class TenantContext {

    private static final ThreadLocal<Context> CONTEXT = ThreadLocal.withInitial(Context::new);

    private TenantContext() {
    }

    public static void setTenantId(String tenantId) {
        CONTEXT.get().tenantId = tenantId;
    }

    public static String getTenantId() {
        return CONTEXT.get().tenantId;
    }

    public static Optional<UUID> getTenantIdAsUuid() {
        String tenantId = getTenantId();
        if (tenantId == null || tenantId.isBlank()) {
            return Optional.empty();
        }
        return Optional.of(UUID.fromString(tenantId));
    }

    public static void setOrganizationId(String organizationId) {
        CONTEXT.get().organizationId = organizationId;
    }

    public static String getOrganizationId() {
        return CONTEXT.get().organizationId;
    }

    public static void setUserId(String userId) {
        CONTEXT.get().userId = userId;
    }

    public static String getUserId() {
        return CONTEXT.get().userId;
    }

    public static void setIsolationMode(TenantIsolationMode mode) {
        CONTEXT.get().isolationMode = mode != null ? mode : TenantIsolationMode.SHARED_SCHEMA;
    }

    public static TenantIsolationMode getIsolationMode() {
        return CONTEXT.get().isolationMode;
    }

    public static void clear() {
        CONTEXT.remove();
    }

    private static final class Context {
        private String tenantId;
        private String organizationId;
        private String userId;
        private TenantIsolationMode isolationMode = TenantIsolationMode.SHARED_SCHEMA;
    }
}
