package com.aisales.tenant.application.service;

import com.aisales.common.contracts.tenant.CreateTenantRequest;
import com.aisales.common.contracts.tenant.TenantDto;
import com.aisales.common.contracts.tenant.TenantStatus;
import com.aisales.common.contracts.tenant.UpdateTenantRequest;
import com.aisales.common.core.util.CorrelationIdUtils;
import com.aisales.common.core.util.TenantContext;
import com.aisales.common.events.model.TenantActivatedEvent;
import com.aisales.common.events.model.TenantCreatedEvent;
import com.aisales.common.events.model.TenantDeletedEvent;
import com.aisales.common.events.model.TenantSuspendedEvent;
import com.aisales.common.events.model.TenantUpdatedEvent;
import com.aisales.common.events.publisher.EventPublisher;
import com.aisales.common.exception.exception.NotFoundException;
import com.aisales.common.exception.exception.ValidationException;
import com.aisales.tenant.application.audit.TenantLifecycleAuditor;
import com.aisales.tenant.application.mapper.TenantMapper;
import com.aisales.tenant.application.metrics.TenantMetrics;
import com.aisales.tenant.application.security.TenantAuthorization;
import com.aisales.tenant.domain.entity.Tenant;
import com.aisales.tenant.domain.repository.TenantRepositoryPort;
import com.aisales.tenant.domain.service.TenantDomainService;
import com.aisales.tenant.domain.service.TenantSlugGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TenantService {

    private final TenantRepositoryPort tenantRepository;
    private final TenantMapper tenantMapper;
    private final TenantDomainService tenantDomainService;
    private final TenantSlugGenerator tenantSlugGenerator;
    private final TenantAuthorization tenantAuthorization;
    private final EventPublisher eventPublisher;
    private final TenantIdempotencyService tenantIdempotencyService;
    private final TenantLifecycleAuditor tenantLifecycleAuditor;
    private final TenantMetrics tenantMetrics;

    @Transactional
    public TenantDto createTenant(CreateTenantRequest request) {
        return createTenant(request, null);
    }

    @Transactional
    public TenantDto createTenant(CreateTenantRequest request, String idempotencyKey) {
        tenantAuthorization.requireSuperAdmin();
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            Optional<TenantDto> cached = tenantIdempotencyService.beginCreate(idempotencyKey);
            if (cached.isPresent()) {
                return cached.get();
            }
        }

        String slug = tenantSlugGenerator.resolveSlug(request.getName(), request.getSlug());

        if (tenantRepository.existsBySlug(slug)) {
            throw new ValidationException("Tenant slug already exists: " + slug);
        }

        String tenantCode = tenantDomainService.resolveTenantCode(request.getTenantCode(), slug);
        if (tenantRepository.existsByTenantCode(tenantCode)) {
            throw new ValidationException("Tenant code already exists: " + tenantCode);
        }

        Tenant tenant = Tenant.builder()
                .tenantCode(tenantCode)
                .name(request.getName())
                .slug(slug)
                .industry(request.getIndustry())
                .subscriptionPlan(tenantDomainService.resolveSubscriptionPlan(request.getSubscriptionPlan()))
                .status(TenantStatus.ACTIVE)
                .timezone(tenantDomainService.resolveTimezone(request.getTimezone()))
                .language(tenantDomainService.resolveLanguage(request.getLanguage()))
                .logoUrl(request.getLogoUrl())
                .deleted(false)
                .build();

        tenant = tenantRepository.save(tenant);
        publishCreatedEvent(tenant);
        TenantDto response = tenantMapper.toDto(tenant);
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            tenantIdempotencyService.storeCreateResponse(idempotencyKey, tenant.getId(), response);
        }
        tenantLifecycleAuditor.tenantCreated(tenant.getId());
        tenantMetrics.recordTenantCreated();
        return response;
    }

    @Transactional(readOnly = true)
    public TenantDto getTenant(UUID id) {
        tenantAuthorization.requireTenantAccess(id);
        return tenantRepository.findActiveById(id)
                .map(tenantMapper::toDto)
                .orElseThrow(() -> new NotFoundException("Tenant", id));
    }

    @Transactional(readOnly = true)
    public TenantDto getTenantBySlug(String slug) {
        Tenant tenant = tenantRepository.findActiveBySlug(slug)
                .orElseThrow(() -> new NotFoundException("Tenant with slug", slug));
        tenantAuthorization.requireTenantAccess(tenant.getId());
        return tenantMapper.toDto(tenant);
    }

    @Transactional(readOnly = true)
    public List<TenantDto> listTenants() {
        tenantAuthorization.requireSuperAdmin();
        return tenantRepository.findAllActive().stream().map(tenantMapper::toDto).toList();
    }

    @Transactional
    public TenantDto updateTenant(UUID id, UpdateTenantRequest request) {
        tenantAuthorization.requireTenantAccess(id);
        Tenant tenant = tenantRepository.findActiveById(id)
                .orElseThrow(() -> new NotFoundException("Tenant", id));

        if (request.getName() != null) {
            if (request.getName().isBlank()) {
                throw new ValidationException("Tenant name cannot be empty");
            }
            tenant.setName(request.getName());
        }
        if (request.getSubscriptionPlan() != null) {
            tenant.setSubscriptionPlan(request.getSubscriptionPlan());
        }
        if (request.getTimezone() != null) {
            tenant.setTimezone(tenantDomainService.resolveTimezone(request.getTimezone()));
        }
        if (request.getLanguage() != null) {
            tenant.setLanguage(tenantDomainService.resolveLanguage(request.getLanguage()));
        }
        if (request.getLogoUrl() != null) {
            tenant.setLogoUrl(request.getLogoUrl());
        }

        tenant = tenantRepository.save(tenant);
        eventPublisher.publish(TenantUpdatedEvent.of(
                tenant.getId().toString(),
                tenant.getName(),
                tenant.getStatus().name(),
                CorrelationIdUtils.getCorrelationId()));
        tenantLifecycleAuditor.tenantUpdated(id);
        tenantMetrics.recordTenantUpdated();
        return tenantMapper.toDto(tenant);
    }

    @Transactional
    public TenantDto activateTenant(UUID id) {
        tenantAuthorization.requireSuperAdmin();
        Tenant tenant = tenantRepository.findActiveById(id)
                .orElseThrow(() -> new NotFoundException("Tenant", id));
        tenantDomainService.activate(tenant);
        tenant = tenantRepository.save(tenant);
        eventPublisher.publish(TenantActivatedEvent.of(
                tenant.getId().toString(),
                tenant.getName(),
                tenant.getSlug(),
                CorrelationIdUtils.getCorrelationId()));
        tenantLifecycleAuditor.tenantActivated(id);
        return tenantMapper.toDto(tenant);
    }

    @Transactional
    public TenantDto suspendTenant(UUID id) {
        tenantAuthorization.requireSuperAdmin();
        Tenant tenant = tenantRepository.findActiveById(id)
                .orElseThrow(() -> new NotFoundException("Tenant", id));
        tenantDomainService.suspend(tenant);
        tenant = tenantRepository.save(tenant);
        eventPublisher.publish(TenantSuspendedEvent.of(
                tenant.getId().toString(),
                tenant.getName(),
                tenant.getSlug(),
                CorrelationIdUtils.getCorrelationId()));
        tenantLifecycleAuditor.tenantSuspended(id);
        return tenantMapper.toDto(tenant);
    }

    @Transactional
    public void deleteTenant(UUID id) {
        tenantAuthorization.requireSuperAdmin();
        Tenant tenant = tenantRepository.findActiveById(id)
                .orElseThrow(() -> new NotFoundException("Tenant", id));
        tenantDomainService.softDelete(tenant, TenantContext.getUserId());
        tenantRepository.save(tenant);
        eventPublisher.publish(TenantDeletedEvent.of(
                tenant.getId().toString(), CorrelationIdUtils.getCorrelationId()));
        tenantLifecycleAuditor.tenantDeleted(id);
        tenantMetrics.recordTenantDeleted();
    }

    private void publishCreatedEvent(Tenant tenant) {
        eventPublisher.publish(TenantCreatedEvent.of(
                tenant.getId().toString(),
                tenant.getName(),
                tenant.getSlug(),
                tenant.getSubscriptionPlan().name(),
                tenant.getIndustry().name(),
                CorrelationIdUtils.getCorrelationId()));
    }
}
