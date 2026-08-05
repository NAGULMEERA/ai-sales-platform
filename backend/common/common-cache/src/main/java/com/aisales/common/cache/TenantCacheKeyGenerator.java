package com.aisales.common.cache;

import com.aisales.common.core.util.TenantContext;
import lombok.RequiredArgsConstructor;

import java.util.UUID;

/**
 * Builds tenant-scoped Redis keys. Platform-wide entries (no tenant in context) use a {@code platform:} segment.
 */
@RequiredArgsConstructor
public class TenantCacheKeyGenerator {

    private final CacheProperties properties;

    public String key(String namespace, String key) {
        String prefix = properties.getKeyPrefix();
        return TenantContext.getTenantIdAsUuid()
                .map(tenantId -> scopedKey(prefix, tenantId, namespace, key))
                .orElseGet(() -> platformKey(prefix, namespace, key));
    }

    public String namespacePattern(String namespace) {
        String prefix = properties.getKeyPrefix();
        return TenantContext.getTenantIdAsUuid()
                .map(tenantId -> prefix + ":tenant:" + tenantId + ":" + namespace + ":*")
                .orElseGet(() -> prefix + ":platform:" + namespace + ":*");
    }

    /**
     * Builds a namespace eviction pattern for an explicit tenant, independent of the calling
     * thread's {@link TenantContext}. Use this when invalidating another tenant's cache entries
     * from a context where the ThreadLocal tenant may be absent or different (e.g. an admin
     * operation or a background job invalidating a specific, caller-supplied tenant).
     */
    public String namespacePatternForTenant(String namespace, UUID tenantId) {
        String prefix = properties.getKeyPrefix();
        if (tenantId == null) {
            return prefix + ":platform:" + namespace + ":*";
        }
        return prefix + ":tenant:" + tenantId + ":" + namespace + ":*";
    }

    public String tenantPattern() {
        String prefix = properties.getKeyPrefix();
        return TenantContext.getTenantIdAsUuid()
                .map(tenantId -> prefix + ":tenant:" + tenantId + ":*")
                .orElseGet(() -> prefix + ":platform:*");
    }

    private static String scopedKey(String prefix, UUID tenantId, String namespace, String key) {
        return prefix + ":tenant:" + tenantId + ":" + namespace + ":" + key;
    }

    private static String platformKey(String prefix, String namespace, String key) {
        return prefix + ":platform:" + namespace + ":" + key;
    }
}
