package com.aisales.identity.authorization.application;

import com.aisales.common.cache.CacheProperties;
import com.aisales.common.cache.PlatformCacheService;
import java.time.Duration;
import java.util.Collections;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.aisales.identity.audit.application.AuditService;
import com.aisales.identity.audit.domain.AuditAction;
import com.aisales.identity.authorization.infrastructure.persistence.PermissionRepository;



/**
 * Resolves role permissions from the global Flyway-seeded catalog. Permissions are cached
 * in Redis when {@code aisales.cache.enabled=true}. Role hierarchy is not implemented;
 * each role's permissions are explicitly assigned in migrations.
 */
@Service
@RequiredArgsConstructor
public class RbacService {

    static final String PERMISSIONS_CACHE_NAMESPACE = "rbac-permissions";

    private final PermissionRepository permissionRepository;
    private final ObjectProvider<PlatformCacheService> cacheService;
    private final ObjectProvider<CacheProperties> cacheProperties;
    private final ObjectProvider<AuditService> auditService;

    @Transactional(readOnly = true)
    public Set<String> resolvePermissions(Set<String> roles) {
        if (roles == null || roles.isEmpty()) {
            return Collections.emptySet();
        }
        Set<String> effectiveRoles = resolveEffectiveRoles(roles);
        String cacheKey = cacheKey(effectiveRoles);
        PlatformCacheService cache = cacheService.getIfAvailable();
        if (cache != null) {
            CachedPermissions cached = cache.getOrLoad(
                    PERMISSIONS_CACHE_NAMESPACE,
                    cacheKey,
                    CachedPermissions.class,
                    () -> new CachedPermissions(loadPermissions(effectiveRoles)),
                    cacheTtl());
            return cached.permissions();
        }
        return loadPermissions(effectiveRoles);
    }

    /**
     * Clears cached permission mappings. Call when role-permission assignments change.
     */
    public void evictPermissionCache() {
        PlatformCacheService cache = cacheService.getIfAvailable();
        if (cache != null) {
            cache.evictNamespace(PERMISSIONS_CACHE_NAMESPACE);
        }
        AuditService audit = auditService.getIfAvailable();
        if (audit != null) {
            audit.logSecurityEvent(null, null, AuditAction.PERMISSION_CACHE_EVICTED, "rbac",
                    PERMISSIONS_CACHE_NAMESPACE, null, null, null);
        }
    }

    /**
     * Placeholder for future role hierarchy. Today roles are flat strings with explicit
     * permission rows in {@code role_permissions}.
     */
    Set<String> resolveEffectiveRoles(Set<String> roles) {
        return roles.stream().collect(Collectors.toCollection(TreeSet::new));
    }

    private Set<String> loadPermissions(Set<String> roles) {
        return permissionRepository.findPermissionCodesByRoleNames(roles);
    }

    private static String cacheKey(Set<String> roles) {
        return String.join(",", roles);
    }

    private Duration cacheTtl() {
        CacheProperties properties = cacheProperties.getIfAvailable();
        return properties != null ? properties.getDefaultTtl() : Duration.ofMinutes(5);
    }

    record CachedPermissions(Set<String> permissions) {
    }
}
