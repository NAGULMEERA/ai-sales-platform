package com.aisales.lead.application.service;

import com.aisales.common.contracts.client.CustomerServiceClient;
import com.aisales.common.contracts.customer.CreateCustomerRequest;
import com.aisales.common.contracts.customer.CustomerDto;
import com.aisales.common.contracts.customer.CustomerSourceType;
import com.aisales.common.core.dto.ApiResponse;
import com.aisales.common.exception.exception.NotFoundException;
import com.aisales.common.exception.exception.ValidationException;
import com.aisales.lead.domain.entity.Lead;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Resolves the customer for lead conversion via customer-service (Feign).
 * Creates a LEAD_CONVERSION customer when none is supplied; reuses by sourceLeadId on retry.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LeadCustomerConversionGateway {

    private final CustomerServiceClient customerServiceClient;

    public UUID resolveCustomerId(Lead lead, UUID requestedCustomerId) {
        if (requestedCustomerId != null) {
            return requireExistingCustomer(requestedCustomerId);
        }
        CustomerDto existing = findBySourceLead(lead.getId());
        if (existing != null) {
            return existing.getId();
        }
        return createFromLead(lead);
    }

    private UUID requireExistingCustomer(UUID customerId) {
        ApiResponse<CustomerDto> response = customerServiceClient.getCustomer(customerId);
        CustomerDto customer = response != null ? response.getData() : null;
        if (customer == null || customer.getId() == null) {
            throw new NotFoundException("Customer not found: " + customerId);
        }
        return customer.getId();
    }

    private CustomerDto findBySourceLead(UUID leadId) {
        try {
            ApiResponse<CustomerDto> response = customerServiceClient.getBySourceLead(leadId);
            return response != null ? response.getData() : null;
        } catch (NotFoundException ex) {
            return null;
        }
    }

    private UUID createFromLead(Lead lead) {
        CreateCustomerRequest request = CreateCustomerRequest.builder()
                .fullName(lead.getCustomerName())
                .phone(lead.getPhone())
                .email(lead.getEmail())
                .sourceType(CustomerSourceType.LEAD_CONVERSION)
                .sourceLeadId(lead.getId())
                .build();
        try {
            ApiResponse<CustomerDto> response = customerServiceClient.createCustomer(request);
            CustomerDto created = response != null ? response.getData() : null;
            if (created == null || created.getId() == null) {
                throw new ValidationException("Customer service did not return a customer id");
            }
            return created.getId();
        } catch (ValidationException ex) {
            // Concurrent convert / retry: customer may already exist for this lead.
            CustomerDto existing = findBySourceLead(lead.getId());
            if (existing != null) {
                log.info("Reusing customer {} for lead {} after create conflict",
                        existing.getId(), lead.getId());
                return existing.getId();
            }
            throw ex;
        }
    }
}
