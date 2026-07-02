package com.aisales.tenant.application.security;

import com.aisales.common.exception.exception.ForbiddenException;
import com.aisales.common.exception.model.ErrorCode;
import com.aisales.common.security.model.UserPrincipal;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.UUID;

@Component("tenantAuthorization")
public class TenantAuthorization {

    private static final String SUPER_ADMIN = "SUPER_ADMIN";
    private static final String TENANT_ADMIN = "TENANT_ADMIN";

    public boolean isSuperAdmin() {
        return hasRole(SUPER_ADMIN);
    }

    public boolean canManageTenant(UUID tenantId) {
        if (isSuperAdmin()) {
            return true;
        }
        UserPrincipal principal = currentPrincipal();
        return hasRole(principal, TENANT_ADMIN) && tenantId.toString().equals(principal.getTenantId());
    }

    public void requireSuperAdmin() {
        if (!isSuperAdmin()) {
            throw new ForbiddenException(ErrorCode.FORBIDDEN, "Super admin access required");
        }
    }

    public void requireTenantAccess(UUID tenantId) {
        if (!canManageTenant(tenantId)) {
            throw new ForbiddenException(ErrorCode.FORBIDDEN, "Tenant access denied");
        }
        if (!isSuperAdmin()) {
            String contextTenantId = com.aisales.common.core.util.TenantContext.getTenantId();
            if (contextTenantId == null || !tenantId.toString().equals(contextTenantId)) {
                throw new ForbiddenException(ErrorCode.TENANT_ACCESS_DENIED, "Tenant context mismatch");
            }
        }
    }

    private boolean hasRole(String role) {
        UserPrincipal principal = currentPrincipal();
        return hasRole(principal, role);
    }

    private boolean hasRole(UserPrincipal principal, String role) {
        Set<String> roles = principal.getRoles();
        return roles != null && roles.contains(role);
    }

    private UserPrincipal currentPrincipal() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof UserPrincipal principal)) {
            throw new ForbiddenException(ErrorCode.UNAUTHORIZED, "Authentication required");
        }
        return principal;
    }
}
