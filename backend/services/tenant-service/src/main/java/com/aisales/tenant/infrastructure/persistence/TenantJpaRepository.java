package com.aisales.tenant.infrastructure.persistence;

import com.aisales.tenant.domain.entity.Tenant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TenantJpaRepository extends JpaRepository<Tenant, UUID> {

    Optional<Tenant> findByIdAndDeletedFalse(UUID id);

    Optional<Tenant> findBySlugAndDeletedFalse(String slug);

    List<Tenant> findAllByDeletedFalse();

    boolean existsBySlug(String slug);

    boolean existsByTenantCode(String tenantCode);
}
