package com.aisales.tenant.application.security;

import com.aisales.common.exception.exception.ForbiddenException;
import com.aisales.common.security.model.UserPrincipal;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TenantAuthorizationTest {

    private TenantAuthorization tenantAuthorization;
    private UUID tenantId;

    @BeforeEach
    void setUp() {
        tenantAuthorization = new TenantAuthorization();
        tenantId = UUID.randomUUID();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldAllowSuperAdminToManageAnyTenant() {
        authenticate(UserPrincipal.builder()
                .userId("admin-1")
                .tenantId(UUID.randomUUID().toString())
                .roles(Set.of("SUPER_ADMIN"))
                .enabled(true)
                .build());

        assertThat(tenantAuthorization.canManageTenant(tenantId)).isTrue();
        assertThat(tenantAuthorization.isSuperAdmin()).isTrue();
    }

    @Test
    void shouldAllowTenantAdminForOwnTenant() {
        authenticate(UserPrincipal.builder()
                .userId("admin-1")
                .tenantId(tenantId.toString())
                .roles(Set.of("TENANT_ADMIN"))
                .enabled(true)
                .build());

        assertThat(tenantAuthorization.canManageTenant(tenantId)).isTrue();
        assertThat(tenantAuthorization.isSuperAdmin()).isFalse();
    }

    @Test
    void shouldDenyTenantAdminForOtherTenant() {
        authenticate(UserPrincipal.builder()
                .userId("admin-1")
                .tenantId(UUID.randomUUID().toString())
                .roles(Set.of("TENANT_ADMIN"))
                .enabled(true)
                .build());

        assertThat(tenantAuthorization.canManageTenant(tenantId)).isFalse();
    }

    @Test
    void shouldDenyTenantAdminWhenContextMismatch() {
        UUID tenantId = UUID.randomUUID();
        authenticate(UserPrincipal.builder()
                .userId("admin-1")
                .tenantId(tenantId.toString())
                .roles(Set.of("TENANT_ADMIN"))
                .enabled(true)
                .build());
        com.aisales.common.core.util.TenantContext.setTenantId(UUID.randomUUID().toString());

        assertThatThrownBy(() -> tenantAuthorization.requireTenantAccess(tenantId))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("context mismatch");
    }

    @Test
    void shouldRequireAuthentication() {
        assertThatThrownBy(() -> tenantAuthorization.requireSuperAdmin())
                .isInstanceOf(ForbiddenException.class);
    }

    private void authenticate(UserPrincipal principal) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
    }
}
