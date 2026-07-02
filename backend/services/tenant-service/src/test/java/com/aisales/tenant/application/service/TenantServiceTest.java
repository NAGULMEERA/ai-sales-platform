package com.aisales.tenant.application.service;

import com.aisales.common.contracts.tenant.CreateTenantRequest;
import com.aisales.common.contracts.tenant.SubscriptionPlan;
import com.aisales.common.contracts.tenant.TenantIndustry;
import com.aisales.common.contracts.tenant.TenantStatus;
import com.aisales.common.contracts.tenant.UpdateTenantRequest;
import com.aisales.common.events.model.TenantActivatedEvent;
import com.aisales.common.events.model.TenantCreatedEvent;
import com.aisales.common.events.model.TenantSuspendedEvent;
import com.aisales.common.events.publisher.EventPublisher;
import com.aisales.common.exception.exception.ForbiddenException;
import com.aisales.common.exception.exception.ValidationException;
import com.aisales.common.security.model.UserPrincipal;
import com.aisales.tenant.application.audit.TenantLifecycleAuditor;
import com.aisales.tenant.application.mapper.TenantMapper;
import com.aisales.tenant.application.metrics.TenantMetrics;
import com.aisales.tenant.application.security.TenantAuthorization;
import com.aisales.tenant.domain.entity.Tenant;
import com.aisales.tenant.domain.repository.TenantRepositoryPort;
import com.aisales.tenant.domain.service.TenantDomainService;
import com.aisales.tenant.domain.service.TenantSlugGenerator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TenantServiceTest {

    @Mock
    private TenantRepositoryPort tenantRepository;
    @Mock
    private TenantMapper tenantMapper;
    @Mock
    private EventPublisher eventPublisher;
    @Mock
    private TenantIdempotencyService tenantIdempotencyService;
    @Mock
    private TenantLifecycleAuditor tenantLifecycleAuditor;
    @Mock
    private TenantMetrics tenantMetrics;

    private TenantService tenantService;
    private TenantSlugGenerator tenantSlugGenerator;

    private TenantAuthorization tenantAuthorization;
    private TenantDomainService tenantDomainService;

    @BeforeEach
    void setUp() {
        tenantAuthorization = new TenantAuthorization();
        tenantDomainService = new TenantDomainService();
        tenantSlugGenerator = new TenantSlugGenerator(tenantRepository, tenantDomainService);
        tenantService = new TenantService(
                tenantRepository, tenantMapper, tenantDomainService, tenantSlugGenerator,
                tenantAuthorization, eventPublisher, tenantIdempotencyService, tenantLifecycleAuditor,
                tenantMetrics);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        com.aisales.common.core.util.TenantContext.clear();
    }

    @Test
    void shouldRejectCreateWhenNotSuperAdmin() {
        authenticateTenantAdmin(UUID.randomUUID());

        assertThatThrownBy(() -> tenantService.createTenant(validCreateRequest()))
                .isInstanceOf(ForbiddenException.class);

        verify(tenantRepository, never()).save(any());
    }

    @Test
    void shouldGenerateUniqueSlugWhenRequestedSlugExists() {
        authenticateSuperAdmin();
        when(tenantRepository.existsBySlug("acme")).thenReturn(true);
        when(tenantRepository.existsBySlug("acme-2")).thenReturn(false);
        when(tenantRepository.existsByTenantCode("ACME")).thenReturn(false);
        when(tenantRepository.save(any())).thenAnswer(invocation -> {
            Tenant tenant = invocation.getArgument(0);
            tenant.setId(UUID.randomUUID());
            return tenant;
        });

        tenantService.createTenant(validCreateRequest());

        verify(tenantRepository).save(org.mockito.ArgumentMatchers.argThat(t -> "acme-2".equals(t.getSlug())));
    }

    @Test
    void shouldCreateTenantAndPublishEvent() {
        authenticateSuperAdmin();
        when(tenantRepository.existsBySlug("acme")).thenReturn(false);
        when(tenantRepository.existsByTenantCode("ACME")).thenReturn(false);
        when(tenantRepository.save(any())).thenAnswer(invocation -> {
            Tenant tenant = invocation.getArgument(0);
            tenant.setId(UUID.randomUUID());
            return tenant;
        });

        tenantService.createTenant(validCreateRequest());

        ArgumentCaptor<TenantCreatedEvent> captor = ArgumentCaptor.forClass(TenantCreatedEvent.class);
        verify(eventPublisher).publish(captor.capture());
        assertThat(captor.getValue().getEventType()).isEqualTo("TenantCreated");
        assertThat(captor.getValue().getIndustry()).isEqualTo("REAL_ESTATE");
    }

    @Test
    void shouldAllowTenantAdminToUpdateOwnTenant() {
        UUID tenantId = UUID.randomUUID();
        authenticateTenantAdmin(tenantId);
        Tenant tenant = sampleTenant(tenantId);
        when(tenantRepository.findActiveById(tenantId)).thenReturn(Optional.of(tenant));
        when(tenantRepository.save(tenant)).thenReturn(tenant);

        tenantService.updateTenant(tenantId, UpdateTenantRequest.builder().name("Updated Name").build());

        assertThat(tenant.getName()).isEqualTo("Updated Name");
        verify(eventPublisher).publish(any());
    }

    @Test
    void shouldPublishActivatedEvent() {
        UUID tenantId = UUID.randomUUID();
        authenticateSuperAdmin();
        Tenant tenant = sampleTenant(tenantId);
        tenant.setStatus(TenantStatus.SUSPENDED);
        when(tenantRepository.findActiveById(tenantId)).thenReturn(Optional.of(tenant));
        when(tenantRepository.save(tenant)).thenReturn(tenant);

        tenantService.activateTenant(tenantId);

        ArgumentCaptor<TenantActivatedEvent> captor = ArgumentCaptor.forClass(TenantActivatedEvent.class);
        verify(eventPublisher).publish(captor.capture());
        assertThat(captor.getValue().getEventType()).isEqualTo("TenantActivated");
        assertThat(tenant.getStatus()).isEqualTo(TenantStatus.ACTIVE);
    }

    @Test
    void shouldPublishSuspendedEvent() {
        UUID tenantId = UUID.randomUUID();
        authenticateSuperAdmin();
        Tenant tenant = sampleTenant(tenantId);
        when(tenantRepository.findActiveById(tenantId)).thenReturn(Optional.of(tenant));
        when(tenantRepository.save(tenant)).thenReturn(tenant);

        tenantService.suspendTenant(tenantId);

        ArgumentCaptor<TenantSuspendedEvent> captor = ArgumentCaptor.forClass(TenantSuspendedEvent.class);
        verify(eventPublisher).publish(captor.capture());
        assertThat(captor.getValue().getEventType()).isEqualTo("TenantSuspended");
        assertThat(tenant.getStatus()).isEqualTo(TenantStatus.SUSPENDED);
    }

    private CreateTenantRequest validCreateRequest() {
        return CreateTenantRequest.builder()
                .name("Acme")
                .slug("acme")
                .tenantCode("ACME")
                .industry(TenantIndustry.REAL_ESTATE)
                .subscriptionPlan(SubscriptionPlan.FREE)
                .build();
    }

    private Tenant sampleTenant(UUID id) {
        Tenant tenant = Tenant.builder()
                .tenantCode("ACME")
                .slug("acme")
                .name("Acme")
                .industry(TenantIndustry.REAL_ESTATE)
                .subscriptionPlan(SubscriptionPlan.FREE)
                .status(TenantStatus.ACTIVE)
                .timezone("UTC")
                .language("en")
                .deleted(false)
                .build();
        tenant.setId(id);
        return tenant;
    }

    private void authenticateSuperAdmin() {
        authenticate(UserPrincipal.builder()
                .userId("super-admin")
                .tenantId(UUID.randomUUID().toString())
                .roles(Set.of("SUPER_ADMIN"))
                .enabled(true)
                .build());
    }

    private void authenticateTenantAdmin(UUID tenantId) {
        com.aisales.common.core.util.TenantContext.setTenantId(tenantId.toString());
        authenticate(UserPrincipal.builder()
                .userId("tenant-admin")
                .tenantId(tenantId.toString())
                .roles(Set.of("TENANT_ADMIN"))
                .enabled(true)
                .build());
    }

    private void authenticate(UserPrincipal principal) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
    }
}
