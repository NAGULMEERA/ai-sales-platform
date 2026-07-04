package com.aisales.identity.authorization.application;

import com.aisales.common.cache.CacheProperties;
import com.aisales.common.cache.PlatformCacheService;
import java.time.Duration;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import com.aisales.identity.audit.application.AuditService;
import com.aisales.identity.audit.domain.AuditAction;
import com.aisales.identity.authorization.infrastructure.persistence.PermissionRepository;



import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RbacServiceTest {

    @Mock private PermissionRepository permissionRepository;
    @Mock private PlatformCacheService platformCacheService;
    @Mock private ObjectProvider<PlatformCacheService> cacheServiceProvider;
    @Mock private ObjectProvider<CacheProperties> cachePropertiesProvider;
    @Mock private ObjectProvider<AuditService> auditServiceProvider;

    private RbacService rbacService;

    @BeforeEach
    void setUp() {
        lenient().when(cacheServiceProvider.getIfAvailable()).thenReturn(null);
        lenient().when(auditServiceProvider.getIfAvailable()).thenReturn(null);
        rbacService = new RbacService(permissionRepository, cacheServiceProvider, cachePropertiesProvider, auditServiceProvider);
    }

    @Test
    void shouldResolvePermissionsForRoles() {
        when(permissionRepository.findPermissionCodesByRoleNames(Set.of("USER")))
                .thenReturn(Set.of("lead:read", "lead:create"));

        Set<String> permissions = rbacService.resolvePermissions(Set.of("USER"));

        assertThat(permissions).containsExactlyInAnyOrder("lead:read", "lead:create");
    }

    @Test
    void shouldReturnEmptyWhenNoRoles() {
        assertThat(rbacService.resolvePermissions(Set.of())).isEmpty();
    }

    @Test
    void shouldUseCacheWhenAvailable() {
        when(cacheServiceProvider.getIfAvailable()).thenReturn(platformCacheService);
        CacheProperties cacheProperties = new CacheProperties();
        cacheProperties.setDefaultTtl(Duration.ofMinutes(10));
        when(cachePropertiesProvider.getIfAvailable()).thenReturn(cacheProperties);
        when(platformCacheService.getOrLoad(
                eq(RbacService.PERMISSIONS_CACHE_NAMESPACE),
                eq("USER"),
                eq(RbacService.CachedPermissions.class),
                any(),
                eq(Duration.ofMinutes(10))))
                .thenReturn(new RbacService.CachedPermissions(Set.of("lead:read")));

        Set<String> permissions = rbacService.resolvePermissions(Set.of("USER"));

        assertThat(permissions).containsExactly("lead:read");
        verify(platformCacheService).getOrLoad(
                eq(RbacService.PERMISSIONS_CACHE_NAMESPACE),
                eq("USER"),
                eq(RbacService.CachedPermissions.class),
                any(),
                eq(Duration.ofMinutes(10)));
    }

    @Test
    void shouldEvictPermissionCacheWhenRequested() {
        PlatformCacheService platformCacheService = org.mockito.Mockito.mock(PlatformCacheService.class);
        AuditService auditService = org.mockito.Mockito.mock(AuditService.class);
        when(cacheServiceProvider.getIfAvailable()).thenReturn(platformCacheService);
        when(auditServiceProvider.getIfAvailable()).thenReturn(auditService);
        rbacService.evictPermissionCache();
        verify(platformCacheService).evictNamespace(RbacService.PERMISSIONS_CACHE_NAMESPACE);
        verify(auditService).logSecurityEvent(
                isNull(), isNull(), eq(AuditAction.PERMISSION_CACHE_EVICTED), eq("rbac"),
                eq(RbacService.PERMISSIONS_CACHE_NAMESPACE), isNull(), isNull(), isNull());
    }
}
