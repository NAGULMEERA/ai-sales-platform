package com.aisales.common.core.persistence;

import com.aisales.common.core.util.TenantContext;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.springframework.stereotype.Component;

/**
 * Enables the Hibernate {@code tenantFilter} on the current persistence session, for services
 * whose entities extend {@link TenantAwareEntity}.
 *
 * <p>Hibernate only registers a {@code @FilterDef} declared on a {@code @MappedSuperclass} when at
 * least one concrete entity in the persistence unit extends it. Services with no such entities
 * (e.g. identity-service, which owns the tenant/user records themselves rather than tenant-scoped
 * business data) will not have "tenantFilter" registered at all; enabling it would otherwise throw
 * {@link org.hibernate.UnknownFilterException} on every authenticated request.
 */
@Slf4j
@Component
public class TenantHibernateFilterEnabler {

    @PersistenceContext
    private EntityManager entityManager;

    public void enableTenantFilter() {
        TenantContext.getTenantIdAsUuid().ifPresent(tenantId -> {
            try {
                Session session = entityManager.unwrap(Session.class);
                session.enableFilter("tenantFilter").setParameter("tenantId", tenantId);
            } catch (HibernateException ex) {
                log.debug("Skipping tenantFilter activation: no entity in this service defines it", ex);
            }
        });
    }
}
