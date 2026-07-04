package com.aisales.identity.tenant.infrastructure.persistence;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import com.aisales.identity.tenant.domain.entity.Tenant;



public interface TenantRepository extends JpaRepository<Tenant, UUID> {

    boolean existsBySlug(String slug);

    Optional<Tenant> findBySlug(String slug);
}
