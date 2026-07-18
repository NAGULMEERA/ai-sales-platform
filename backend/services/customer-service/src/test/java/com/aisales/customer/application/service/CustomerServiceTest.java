package com.aisales.customer.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.aisales.common.contracts.customer.CreateCustomerAddressRequest;
import com.aisales.common.contracts.customer.CreateCustomerRequest;
import com.aisales.common.contracts.customer.CustomerAddressDto;
import com.aisales.common.contracts.customer.CustomerDto;
import com.aisales.common.contracts.customer.CustomerSourceType;
import com.aisales.common.contracts.customer.CustomerStatus;
import com.aisales.common.core.util.TenantContext;
import com.aisales.common.events.model.CustomerArchivedEvent;
import com.aisales.common.events.model.CustomerCreatedEvent;
import com.aisales.common.events.publisher.EventPublisher;
import com.aisales.common.exception.exception.NotFoundException;
import com.aisales.common.exception.exception.ValidationException;
import com.aisales.customer.application.mapper.CustomerMapper;
import com.aisales.customer.domain.entity.Customer;
import com.aisales.customer.domain.entity.CustomerAddress;
import com.aisales.customer.infrastructure.persistence.CustomerAddressRepository;
import com.aisales.customer.infrastructure.persistence.CustomerRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CustomerServiceTest {

    @Mock private CustomerRepository customerRepository;
    @Mock private CustomerAddressRepository addressRepository;
    @Mock private EventPublisher eventPublisher;

    private CustomerService customerService;
    private UUID tenantId;

    @BeforeEach
    void setUp() {
        tenantId = UUID.randomUUID();
        TenantContext.setTenantId(tenantId.toString());
        TenantContext.setUserId(UUID.randomUUID().toString());
        customerService = new CustomerService(
                customerRepository, addressRepository, new CustomerMapper(), eventPublisher);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void shouldCreateCustomerAndPublishEvent() {
        when(customerRepository.saveAndFlush(any(Customer.class))).thenAnswer(inv -> {
            Customer c = inv.getArgument(0);
            c.setId(UUID.randomUUID());
            return c;
        });

        CustomerDto dto = customerService.createCustomer(CreateCustomerRequest.builder()
                .fullName("Jane Customer")
                .phone("+919999999999")
                .email("jane@example.com")
                .sourceType(CustomerSourceType.MANUAL)
                .build());

        assertThat(dto.getFullName()).isEqualTo("Jane Customer");
        assertThat(dto.getStatus()).isEqualTo(CustomerStatus.PROSPECT);
        assertThat(dto.getTenantId()).isEqualTo(tenantId);

        ArgumentCaptor<CustomerCreatedEvent> captor = ArgumentCaptor.forClass(CustomerCreatedEvent.class);
        verify(eventPublisher).publish(captor.capture());
        assertThat(captor.getValue().getEventType()).isEqualTo("CustomerCreated");
    }

    @Test
    void shouldRequireContactMethod() {
        assertThatThrownBy(() -> customerService.createCustomer(CreateCustomerRequest.builder()
                        .fullName("No Contact")
                        .build()))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("contact method");
    }

    @Test
    void shouldRequireLeadIdForLeadConversion() {
        assertThatThrownBy(() -> customerService.createCustomer(CreateCustomerRequest.builder()
                        .fullName("From Lead")
                        .phone("+911111111111")
                        .sourceType(CustomerSourceType.LEAD_CONVERSION)
                        .build()))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("sourceLeadId");
    }

    @Test
    void shouldRejectDuplicateLeadConversion() {
        UUID leadId = UUID.randomUUID();
        when(customerRepository.findByTenantIdAndSourceLeadIdAndDeletedAtIsNull(tenantId, leadId))
                .thenReturn(Optional.of(Customer.builder().id(UUID.randomUUID()).build()));

        assertThatThrownBy(() -> customerService.createCustomer(CreateCustomerRequest.builder()
                        .fullName("Dup")
                        .phone("+911111111111")
                        .sourceType(CustomerSourceType.LEAD_CONVERSION)
                        .sourceLeadId(leadId)
                        .build()))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("already exists");
    }

    @Test
    void shouldActivateCustomer() {
        UUID customerId = UUID.randomUUID();
        Customer customer = Customer.builder()
                .id(customerId)
                .tenantId(tenantId)
                .fullName("Ada")
                .phone("+911234567890")
                .status(CustomerStatus.PROSPECT)
                .sourceType(CustomerSourceType.MANUAL)
                .build();
        when(customerRepository.findByTenantIdAndIdAndDeletedAtIsNull(tenantId, customerId))
                .thenReturn(Optional.of(customer));
        when(customerRepository.save(any(Customer.class))).thenAnswer(inv -> inv.getArgument(0));

        CustomerDto dto = customerService.activate(customerId);

        assertThat(dto.getStatus()).isEqualTo(CustomerStatus.ACTIVE);
    }

    @Test
    void shouldArchiveCustomerAndPublishEvent() {
        UUID customerId = UUID.randomUUID();
        Customer customer = Customer.builder()
                .id(customerId)
                .tenantId(tenantId)
                .fullName("Ada")
                .email("ada@example.com")
                .status(CustomerStatus.ACTIVE)
                .sourceType(CustomerSourceType.MANUAL)
                .build();
        when(customerRepository.findByTenantIdAndIdAndDeletedAtIsNull(tenantId, customerId))
                .thenReturn(Optional.of(customer));
        when(customerRepository.save(any(Customer.class))).thenAnswer(inv -> inv.getArgument(0));

        customerService.archive(customerId, "churn");

        ArgumentCaptor<CustomerArchivedEvent> captor = ArgumentCaptor.forClass(CustomerArchivedEvent.class);
        verify(eventPublisher).publish(captor.capture());
        assertThat(captor.getValue().getReason()).isEqualTo("churn");
        assertThat(customer.getStatus()).isEqualTo(CustomerStatus.ARCHIVED);
    }

    @Test
    void shouldNotExposeOtherTenantCustomer() {
        when(customerRepository.findByTenantIdAndIdAndDeletedAtIsNull(eq(tenantId), any(UUID.class)))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> customerService.getCustomer(UUID.randomUUID()))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void shouldFindCustomerBySourceLead() {
        UUID leadId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        when(customerRepository.findByTenantIdAndSourceLeadIdAndDeletedAtIsNull(tenantId, leadId))
                .thenReturn(Optional.of(Customer.builder()
                        .id(customerId)
                        .tenantId(tenantId)
                        .fullName("From Lead")
                        .phone("+911111111111")
                        .sourceType(CustomerSourceType.LEAD_CONVERSION)
                        .sourceLeadId(leadId)
                        .status(CustomerStatus.PROSPECT)
                        .build()));

        CustomerDto dto = customerService.getBySourceLead(leadId);
        assertThat(dto.getId()).isEqualTo(customerId);
        assertThat(dto.getSourceLeadId()).isEqualTo(leadId);
    }

    @Test
    void shouldAddPrimaryAddress() {
        UUID customerId = UUID.randomUUID();
        Customer customer = Customer.builder()
                .id(customerId)
                .tenantId(tenantId)
                .fullName("Ada")
                .phone("+911234567890")
                .status(CustomerStatus.ACTIVE)
                .sourceType(CustomerSourceType.MANUAL)
                .build();
        when(customerRepository.findByTenantIdAndIdAndDeletedAtIsNull(tenantId, customerId))
                .thenReturn(Optional.of(customer));
        when(addressRepository.saveAndFlush(any(CustomerAddress.class))).thenAnswer(inv -> {
            CustomerAddress a = inv.getArgument(0);
            a.setId(UUID.randomUUID());
            return a;
        });

        CustomerAddressDto address = customerService.addAddress(customerId, CreateCustomerAddressRequest.builder()
                .line1("12 MG Road")
                .city("Bengaluru")
                .state("KA")
                .postalCode("560001")
                .primary(true)
                .build());

        assertThat(address.getCity()).isEqualTo("Bengaluru");
        assertThat(address.isPrimary()).isTrue();
        verify(addressRepository).clearPrimaryFlags(tenantId, customerId);
    }
}
