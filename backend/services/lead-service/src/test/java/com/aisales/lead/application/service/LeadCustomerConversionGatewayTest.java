package com.aisales.lead.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.aisales.common.contracts.client.CustomerServiceClient;
import com.aisales.common.contracts.customer.CreateCustomerRequest;
import com.aisales.common.contracts.customer.CustomerDto;
import com.aisales.common.contracts.customer.CustomerSourceType;
import com.aisales.common.core.dto.ApiResponse;
import com.aisales.common.exception.exception.NotFoundException;
import com.aisales.lead.domain.entity.Lead;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class LeadCustomerConversionGatewayTest {

    @Mock private CustomerServiceClient customerServiceClient;

    private LeadCustomerConversionGateway gateway;
    private Lead lead;

    @BeforeEach
    void setUp() {
        gateway = new LeadCustomerConversionGateway(customerServiceClient);
        lead = Lead.builder()
                .id(UUID.randomUUID())
                .tenantId(UUID.randomUUID())
                .customerName("Ada Lovelace")
                .phone("+919876543210")
                .email("ada@example.com")
                .build();
    }

    @Test
    void shouldValidateExistingCustomerId() {
        UUID customerId = UUID.randomUUID();
        when(customerServiceClient.getCustomer(customerId))
                .thenReturn(ApiResponse.ok(CustomerDto.builder().id(customerId).build()));

        UUID resolved = gateway.resolveCustomerId(lead, customerId);

        assertThat(resolved).isEqualTo(customerId);
        verify(customerServiceClient, never()).createCustomer(any());
    }

    @Test
    void shouldCreateCustomerWhenNoneProvided() {
        UUID createdId = UUID.randomUUID();
        when(customerServiceClient.getBySourceLead(lead.getId()))
                .thenThrow(new NotFoundException("missing"));
        when(customerServiceClient.createCustomer(any(CreateCustomerRequest.class)))
                .thenReturn(ApiResponse.ok(CustomerDto.builder().id(createdId).build()));

        UUID resolved = gateway.resolveCustomerId(lead, null);

        assertThat(resolved).isEqualTo(createdId);
        ArgumentCaptor<CreateCustomerRequest> captor = ArgumentCaptor.forClass(CreateCustomerRequest.class);
        verify(customerServiceClient).createCustomer(captor.capture());
        assertThat(captor.getValue().getSourceType()).isEqualTo(CustomerSourceType.LEAD_CONVERSION);
        assertThat(captor.getValue().getSourceLeadId()).isEqualTo(lead.getId());
        assertThat(captor.getValue().getFullName()).isEqualTo("Ada Lovelace");
    }

    @Test
    void shouldReuseCustomerBySourceLeadOnRetry() {
        UUID existingId = UUID.randomUUID();
        when(customerServiceClient.getBySourceLead(lead.getId()))
                .thenReturn(ApiResponse.ok(CustomerDto.builder().id(existingId).build()));

        UUID resolved = gateway.resolveCustomerId(lead, null);

        assertThat(resolved).isEqualTo(existingId);
        verify(customerServiceClient, never()).createCustomer(any());
    }

    @Test
    void shouldRejectUnknownCustomerId() {
        UUID customerId = UUID.randomUUID();
        when(customerServiceClient.getCustomer(customerId))
                .thenThrow(new NotFoundException("Customer not found"));

        assertThatThrownBy(() -> gateway.resolveCustomerId(lead, customerId))
                .isInstanceOf(NotFoundException.class);
    }
}
