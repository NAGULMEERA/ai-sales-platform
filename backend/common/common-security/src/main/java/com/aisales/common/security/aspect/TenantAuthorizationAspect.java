package com.aisales.common.security.aspect;

import com.aisales.common.core.util.TenantContext;
import com.aisales.common.exception.exception.ForbiddenException;
import com.aisales.common.exception.model.ErrorCode;
import com.aisales.common.security.annotation.PreAuthorizeTenant;
import com.aisales.common.security.model.UserPrincipal;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * Enforces that the authenticated user's tenant matches {@link TenantContext}.
 */
@Aspect
@Component
public class TenantAuthorizationAspect {

    @Before("@within(preAuthorizeTenant) || @annotation(preAuthorizeTenant)")
    public void verifyTenantAccess(PreAuthorizeTenant preAuthorizeTenant) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof UserPrincipal principal)) {
            throw new ForbiddenException(ErrorCode.UNAUTHORIZED, "Authentication required");
        }

        String contextTenantId = TenantContext.getTenantId();
        if (contextTenantId == null || !contextTenantId.equals(principal.getTenantId())) {
            throw new ForbiddenException(ErrorCode.TENANT_ACCESS_DENIED, "Cross-tenant access denied");
        }
    }
}
