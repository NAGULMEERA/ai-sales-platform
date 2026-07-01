package com.aisales.tenant.application.service;

import com.aisales.common.contracts.tenant.CreateTenantRequest;
import com.aisales.common.contracts.tenant.TenantDto;
import com.aisales.common.contracts.tenant.UpdateTenantRequest;
import com.aisales.common.core.util.CorrelationIdUtils;
import com.aisales.common.events.model.TenantCreatedEvent;
import com.aisales.common.events.model.TenantDeletedEvent;
import com.aisales.common.events.model.TenantUpdatedEvent;
import com.aisales.common.events.publisher.EventPublisher;
import com.aisales.common.exception.exception.NotFoundException;
import com.aisales.common.exception.exception.ValidationException;
import com.aisales.tenant.application.mapper.TenantMapper;
import com.aisales.tenant.domain.entity.Tenant;
import com.aisales.tenant.infrastructure.persistence.TenantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TenantService {

    private final TenantRepository tenantRepository;
    private final TenantMapper tenantMapper;
    private final EventPublisher eventPublisher;

    @Transactional
    public TenantDto createTenant(CreateTenantRequest request) {
        if (tenantRepository.existsBySlug(request.getSlug())) {
            throw new ValidationException("Tenant slug already exists: " + request.getSlug());
        }
        Tenant tenant = Tenant.builder()
                .name(request.getName())
                .slug(request.getSlug())
                .plan(request.getPlan())
                .status(Tenant.TenantStatus.ACTIVE)
                .build();
        tenant = tenantRepository.save(tenant);
        eventPublisher.publish(TenantCreatedEvent.of(
                tenant.getId().toString(), tenant.getName(), tenant.getPlan(),
                CorrelationIdUtils.getCorrelationId()));
        return tenantMapper.toDto(tenant);
    }

    @Transactional(readOnly = true)
    public TenantDto getTenant(UUID id) {
        return tenantRepository.findById(id)
                .map(tenantMapper::toDto)
                .orElseThrow(() -> new NotFoundException("Tenant", id));
    }

    @Transactional(readOnly = true)
    public TenantDto getTenantBySlug(String slug) {
        return tenantRepository.findBySlug(slug)
                .map(tenantMapper::toDto)
                .orElseThrow(() -> new NotFoundException("Tenant with slug", slug));
    }

    @Transactional(readOnly = true)
    public List<TenantDto> listTenants() {
        return tenantRepository.findAll().stream().map(tenantMapper::toDto).toList();
    }

    @Transactional
    public TenantDto updateTenant(UUID id, UpdateTenantRequest request) {
        Tenant tenant = tenantRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Tenant", id));
        if (request.getName() != null) {
            tenant.setName(request.getName());
        }
        if (request.getPlan() != null) {
            tenant.setPlan(request.getPlan());
        }
        if (request.getStatus() != null) {
            tenant.setStatus(Tenant.TenantStatus.valueOf(request.getStatus()));
        }
        tenant = tenantRepository.save(tenant);
        eventPublisher.publish(TenantUpdatedEvent.of(
                tenant.getId().toString(), tenant.getName(), tenant.getStatus().name(),
                CorrelationIdUtils.getCorrelationId()));
        return tenantMapper.toDto(tenant);
    }

    @Transactional
    public void deleteTenant(UUID id) {
        Tenant tenant = tenantRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Tenant", id));
        tenant.setStatus(Tenant.TenantStatus.DELETED);
        tenantRepository.save(tenant);
        eventPublisher.publish(TenantDeletedEvent.of(
                tenant.getId().toString(), CorrelationIdUtils.getCorrelationId()));
    }
}
