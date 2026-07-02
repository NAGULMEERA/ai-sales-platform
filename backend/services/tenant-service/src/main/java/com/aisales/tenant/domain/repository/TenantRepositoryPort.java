package com.aisales.tenant.domain.repository;

import com.aisales.tenant.domain.entity.Tenant;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TenantRepositoryPort {

    Tenant save(Tenant tenant);

    Optional<Tenant> findById(UUID id);

    Optional<Tenant> findActiveById(UUID id);

    Optional<Tenant> findActiveBySlug(String slug);

    List<Tenant> findAllActive();

    boolean existsBySlug(String slug);

    boolean existsByTenantCode(String tenantCode);
}
