package com.aisales.common.security.aspect;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.aisales.common.core.util.TenantContext;
import com.aisales.common.exception.exception.ForbiddenException;
import com.aisales.common.security.annotation.PreAuthorizeTenant;
import com.aisales.common.security.model.UserPrincipal;
import java.lang.annotation.Annotation;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

class TenantAuthorizationAspectTest {

    private final TenantAuthorizationAspect aspect = new TenantAuthorizationAspect();
    private final PreAuthorizeTenant annotation = new PreAuthorizeTenant() {
        @Override
        public Class<? extends Annotation> annotationType() {
            return PreAuthorizeTenant.class;
        }

        @Override
        public String value() {
            return "";
        }
    };

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        TenantContext.clear();
    }

    @Test
    void shouldAllowWhenPrincipalTenantMatchesContext() {
        authenticate("tenant-a");
        TenantContext.setTenantId("tenant-a");

        assertThatCode(() -> aspect.verifyTenantAccess(annotation)).doesNotThrowAnyException();
    }

    @Test
    void shouldDenyCrossTenantAccess() {
        authenticate("tenant-a");
        TenantContext.setTenantId("tenant-b");

        assertThatThrownBy(() -> aspect.verifyTenantAccess(annotation))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("Cross-tenant");
    }

    @Test
    void shouldDenyWhenUnauthenticated() {
        TenantContext.setTenantId("tenant-a");

        assertThatThrownBy(() -> aspect.verifyTenantAccess(annotation))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("Authentication required");
    }

    @Test
    void shouldDenyWhenTenantContextMissing() {
        authenticate("tenant-a");

        assertThatThrownBy(() -> aspect.verifyTenantAccess(annotation))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("Cross-tenant");
    }

    private static void authenticate(String tenantId) {
        UserPrincipal principal = UserPrincipal.builder()
                .userId("user-1")
                .tenantId(tenantId)
                .email("a@b.com")
                .roles(Set.of("AGENT"))
                .permissions(Set.of("lead:read"))
                .enabled(true)
                .build();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
    }
}
