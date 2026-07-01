package com.aisales.common.core.persistence;

import com.aisales.common.core.util.TenantContext;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Sets PostgreSQL session variable {@code app.current_tenant} for Row-Level Security policies.
 */
@Aspect
@Component
@Order(0)
public class TenantRlsConnectionInitializer {

    private static final String SET_TENANT_SQL =
            "SELECT set_config('app.current_tenant', :tenantId, true)";

    @PersistenceContext
    private EntityManager entityManager;

    @Before("@annotation(transactional)")
    public void applyRlsContext(Transactional transactional) {
        TenantContext.getTenantIdAsUuid().ifPresent(tenantId ->
                entityManager.createNativeQuery(SET_TENANT_SQL)
                        .setParameter("tenantId", tenantId.toString())
                        .getSingleResult());
    }
}
