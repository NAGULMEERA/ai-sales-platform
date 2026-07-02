package com.aisales.tenant.infrastructure.persistence;

import com.aisales.tenant.domain.entity.Tenant;
import com.aisales.tenant.domain.repository.TenantRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class TenantRepositoryAdapter implements TenantRepositoryPort {

    private final TenantJpaRepository tenantJpaRepository;

    @Override
    public Tenant save(Tenant tenant) {
        return tenantJpaRepository.save(tenant);
    }

    @Override
    public Optional<Tenant> findById(UUID id) {
        return tenantJpaRepository.findById(id);
    }

    @Override
    public Optional<Tenant> findActiveById(UUID id) {
        return tenantJpaRepository.findByIdAndDeletedFalse(id);
    }

    @Override
    public Optional<Tenant> findActiveBySlug(String slug) {
        return tenantJpaRepository.findBySlugAndDeletedFalse(slug);
    }

    @Override
    public List<Tenant> findAllActive() {
        return tenantJpaRepository.findAllByDeletedFalse();
    }

    @Override
    public boolean existsBySlug(String slug) {
        return tenantJpaRepository.existsBySlug(slug);
    }

    @Override
    public boolean existsByTenantCode(String tenantCode) {
        return tenantJpaRepository.existsByTenantCode(tenantCode);
    }
}
