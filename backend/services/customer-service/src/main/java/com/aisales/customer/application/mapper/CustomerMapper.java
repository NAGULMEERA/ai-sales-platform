package com.aisales.customer.application.mapper;

import com.aisales.common.contracts.customer.CustomerAddressDto;
import com.aisales.common.contracts.customer.CustomerDto;
import com.aisales.customer.domain.entity.Customer;
import com.aisales.customer.domain.entity.CustomerAddress;
import java.util.HashMap;
import org.springframework.stereotype.Component;

@Component
public class CustomerMapper {

    public CustomerDto toDto(Customer customer) {
        return CustomerDto.builder()
                .id(customer.getId())
                .tenantId(customer.getTenantId())
                .organizationId(customer.getOrganizationId())
                .fullName(customer.getFullName())
                .phone(customer.getPhone())
                .email(customer.getEmail())
                .status(customer.getStatus())
                .sourceType(customer.getSourceType())
                .sourceLeadId(customer.getSourceLeadId())
                .metadata(customer.getMetadata() == null
                        ? new HashMap<>()
                        : new HashMap<>(customer.getMetadata()))
                .createdAt(customer.getCreatedAt())
                .updatedAt(customer.getUpdatedAt())
                .version(customer.getVersion())
                .build();
    }

    public CustomerAddressDto toAddressDto(CustomerAddress address) {
        return CustomerAddressDto.builder()
                .id(address.getId())
                .customerId(address.getCustomerId())
                .addressType(address.getAddressType())
                .line1(address.getLine1())
                .line2(address.getLine2())
                .city(address.getCity())
                .state(address.getState())
                .postalCode(address.getPostalCode())
                .country(address.getCountry())
                .primary(address.isPrimaryAddress())
                .createdAt(address.getCreatedAt())
                .updatedAt(address.getUpdatedAt())
                .build();
    }
}
