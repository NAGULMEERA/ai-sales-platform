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
