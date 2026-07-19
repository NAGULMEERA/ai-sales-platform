package com.aisales.customer.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.aisales.common.contracts.customer.IdentityMatchType;
import com.aisales.common.contracts.customer.MergeCustomerRequest;
import com.aisales.common.contracts.customer.ResolveIdentityRequest;
import com.aisales.common.contracts.customer.CustomerSourceType;
import com.aisales.common.contracts.customer.CustomerStatus;
import com.aisales.common.core.util.TenantContext;
import com.aisales.common.events.model.CustomerMergedEvent;
import com.aisales.common.events.publisher.EventPublisher;
import com.aisales.customer.application.mapper.CustomerMapper;
import com.aisales.customer.config.CustomerMatchingProperties;
import com.aisales.customer.domain.entity.Customer;
import com.aisales.customer.infrastructure.persistence.CustomerAddressRepository;
import com.aisales.customer.infrastructure.persistence.CustomerConsentRepository;
import com.aisales.customer.infrastructure.persistence.CustomerContactMethodRepository;
import com.aisales.customer.infrastructure.persistence.CustomerDuplicateRepository;
import com.aisales.customer.infrastructure.persistence.CustomerInteractionRepository;
import com.aisales.customer.infrastructure.persistence.CustomerRepository;
import com.aisales.customer.infrastructure.persistence.CustomerTimelineRepository;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.beans.factory.ObjectProvider;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CustomerIdentityAndMergeTest {

    @Mock private CustomerRepository customerRepository;
    @Mock private CustomerAddressRepository addressRepository;
    @Mock private CustomerContactMethodRepository contactMethodRepository;
    @Mock private CustomerConsentRepository consentRepository;
    @Mock private CustomerTimelineRepository timelineRepository;
    @Mock private CustomerInteractionRepository interactionRepository;
    @Mock private CustomerDuplicateRepository duplicateRepository;
    @Mock private EventPublisher eventPublisher;
    @Mock private CustomerTimelineRecorder timelineRecorder;
    @Mock private CustomerDuplicateDetectionService duplicateDetection;
    @Mock private CustomerIdempotencyService idempotencyService;
    @Mock private ObjectProvider<?> platformMetrics;

    private CustomerIdentityResolutionService identityService;
    private CustomerService customerService;
    private UUID tenantId;

    @BeforeEach
    void setUp() {
        tenantId = UUID.randomUUID();
        TenantContext.setTenantId(tenantId.toString());
        TenantContext.setUserId(UUID.randomUUID().toString());
        CustomerMatchingProperties props = new CustomerMatchingProperties();
        identityService = new CustomerIdentityResolutionService(customerRepository, props);
        when(idempotencyService.beginCreate(any())).thenReturn(Optional.empty());
        when(platformMetrics.getIfAvailable()).thenReturn(null);
        customerService = new CustomerService(
                customerRepository,
                addressRepository,
                contactMethodRepository,
                consentRepository,
                timelineRepository,
                interactionRepository,
                duplicateRepository,
                new CustomerMapper(),
                eventPublisher,
                timelineRecorder,
                duplicateDetection,
                identityService,
                idempotencyService,
                (ObjectProvider) platformMetrics);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void shouldResolveExactMatchByEmail() {
        UUID customerId = UUID.randomUUID();
        when(customerRepository.findIdentityCandidates(
                        eq(tenantId), isNull(), eq("a@example.com"), isNull(), isNull(), isNull(), isNull()))
                .thenReturn(List.of(Customer.builder()
                        .id(customerId)
                        .tenantId(tenantId)
                        .fullName("Ada")
                        .email("a@example.com")
                        .phone("+911111111111")
                        .status(CustomerStatus.ACTIVE)
                        .sourceType(CustomerSourceType.MANUAL)
                        .build()));

        var result = identityService.resolve(ResolveIdentityRequest.builder()
                .email("a@example.com")
                .build());

        assertThat(result.getMatchType()).isEqualTo(IdentityMatchType.EXACT_MATCH);
        assertThat(result.getCandidates()).hasSize(1);
        assertThat(result.getCandidates().get(0).getMatchedOn()).contains("EMAIL");
    }

    @Test
    void shouldReturnNoMatchWhenEmpty() {
        when(customerRepository.findIdentityCandidates(
                        eq(tenantId), eq("+911"), isNull(), isNull(), isNull(), isNull(), isNull()))
                .thenReturn(List.of());

        var result = identityService.resolve(ResolveIdentityRequest.builder().phone("+911").build());
        assertThat(result.getMatchType()).isEqualTo(IdentityMatchType.NO_MATCH);
    }

    @Test
    void shouldMergeLoserIntoSurvivorPreservingHistory() {
        UUID survivorId = UUID.randomUUID();
        UUID loserId = UUID.randomUUID();
        Customer survivor = Customer.builder()
                .id(survivorId)
                .tenantId(tenantId)
                .fullName("Survivor")
                .phone("+911111111111")
                .status(CustomerStatus.ACTIVE)
                .sourceType(CustomerSourceType.MANUAL)
                .metadata(new HashMap<>())
                .preferences(new HashMap<>())
                .build();
        Customer loser = Customer.builder()
                .id(loserId)
                .tenantId(tenantId)
                .fullName("Loser")
                .phone("+922222222222")
                .email("loser@example.com")
                .whatsapp("+933333333333")
                .status(CustomerStatus.PROSPECT)
                .sourceType(CustomerSourceType.MANUAL)
                .metadata(new HashMap<>(java.util.Map.of("crmNote", "keep")))
                .preferences(new HashMap<>())
                .build();

        when(customerRepository.findByTenantIdAndIdAndDeletedAtIsNull(tenantId, survivorId))
                .thenReturn(Optional.of(survivor));
        when(customerRepository.findByTenantIdAndIdAndDeletedAtIsNull(tenantId, loserId))
                .thenReturn(Optional.of(loser));
        when(customerRepository.save(any(Customer.class))).thenAnswer(inv -> inv.getArgument(0));
        when(duplicateRepository.findByTenantIdAndCustomerIdAndResolvedFalse(any(), any()))
                .thenReturn(List.of());

        var dto = customerService.mergeCustomer(
                survivorId, MergeCustomerRequest.builder().loserCustomerId(loserId).reason("dup").build());

        assertThat(dto.getEmail()).isEqualTo("loser@example.com");
        assertThat(dto.getWhatsapp()).isEqualTo("+933333333333");
        assertThat(dto.getMetadata()).containsEntry("crmNote", "keep");
        assertThat(loser.getDeletedAt()).isNotNull();
        assertThat(loser.getMergedIntoCustomerId()).isEqualTo(survivorId);
        verify(timelineRepository).reassignToSurvivor(tenantId, loserId, survivorId);
        verify(interactionRepository).reassignToSurvivor(tenantId, loserId, survivorId);
        verify(addressRepository).reassignToSurvivor(tenantId, loserId, survivorId);

        ArgumentCaptor<CustomerMergedEvent> captor = ArgumentCaptor.forClass(CustomerMergedEvent.class);
        verify(eventPublisher).publish(captor.capture());
        assertThat(captor.getValue().getEventType()).isEqualTo("CustomerMerged");
    }
}
