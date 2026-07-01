package com.aisales.common.core.persistence;

import com.aisales.common.core.util.TenantContext;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.hibernate.Session;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Enables Hibernate tenant filter on the current persistence session.
 */
@Component
public class TenantHibernateFilterEnabler {

    @PersistenceContext
    private EntityManager entityManager;

    public void enableTenantFilter() {
        TenantContext.getTenantIdAsUuid().ifPresent(tenantId -> {
            Session session = entityManager.unwrap(Session.class);
            session.enableFilter("tenantFilter").setParameter("tenantId", tenantId);
        });
    }
}
