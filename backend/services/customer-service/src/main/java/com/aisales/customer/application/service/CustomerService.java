package com.aisales.customer.application.service;

import com.aisales.common.contracts.customer.CreateCustomerAddressRequest;
import com.aisales.common.contracts.customer.CreateCustomerRequest;
import com.aisales.common.contracts.customer.CustomerAddressDto;
import com.aisales.common.contracts.customer.CustomerDto;
import com.aisales.common.contracts.customer.CustomerSourceType;
import com.aisales.common.contracts.customer.CustomerStatus;
import com.aisales.common.contracts.customer.UpdateCustomerAddressRequest;
import com.aisales.common.contracts.customer.UpdateCustomerRequest;
import com.aisales.common.core.dto.PageResponse;
import com.aisales.common.core.util.CorrelationIdUtils;
import com.aisales.common.core.util.TenantContext;
import com.aisales.common.events.model.CustomerArchivedEvent;
import com.aisales.common.events.model.CustomerCreatedEvent;
import com.aisales.common.events.model.CustomerUpdatedEvent;
import com.aisales.common.events.publisher.EventPublisher;
import com.aisales.common.exception.exception.NotFoundException;
import com.aisales.common.exception.exception.ValidationException;
import com.aisales.customer.application.mapper.CustomerMapper;
import com.aisales.customer.domain.entity.Customer;
import com.aisales.customer.domain.entity.CustomerAddress;
import com.aisales.customer.infrastructure.persistence.CustomerAddressRepository;
import com.aisales.customer.infrastructure.persistence.CustomerRepository;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final CustomerAddressRepository addressRepository;
    private final CustomerMapper mapper;
    private final EventPublisher eventPublisher;

    @Transactional
    public CustomerDto createCustomer(CreateCustomerRequest request) {
        UUID tenantId = requireTenantId();
        UUID actor = actorId();
        Instant now = Instant.now();

        CustomerSourceType sourceType = request.getSourceType() != null
                ? request.getSourceType()
                : CustomerSourceType.MANUAL;

        if (sourceType == CustomerSourceType.LEAD_CONVERSION && request.getSourceLeadId() == null) {
            throw new ValidationException("sourceLeadId is required for LEAD_CONVERSION");
        }
        if (request.getSourceLeadId() != null) {
            customerRepository.findByTenantIdAndSourceLeadIdAndDeletedAtIsNull(tenantId, request.getSourceLeadId())
                    .ifPresent(existing -> {
                        throw new ValidationException(
                                "Customer already exists for lead: " + request.getSourceLeadId());
                    });
        }

        Customer customer = Customer.builder()
                .tenantId(tenantId)
                .organizationId(parseUuidOrNull(TenantContext.getOrganizationId()))
                .fullName(request.getFullName().trim())
                .phone(trimToNull(request.getPhone()))
                .email(trimToNull(request.getEmail()))
                .status(CustomerStatus.PROSPECT)
                .sourceType(sourceType)
                .sourceLeadId(request.getSourceLeadId())
                .metadata(request.getMetadata() != null
                        ? new HashMap<>(request.getMetadata())
                        : new HashMap<>())
                .createdAt(now)
                .updatedAt(now)
                .createdBy(actor)
                .updatedBy(actor)
                .build();
        customer.assertHasContactMethod();

        Customer saved = customerRepository.saveAndFlush(customer);
        eventPublisher.publish(CustomerCreatedEvent.of(
                tenantId.toString(),
                saved.getId().toString(),
                saved.getFullName(),
                saved.getEmail(),
                correlationId()));
        return mapper.toDto(saved);
    }

    @Transactional(readOnly = true)
    public CustomerDto getCustomer(UUID customerId) {
        return mapper.toDto(requireCustomer(customerId));
    }

    @Transactional(readOnly = true)
    public CustomerDto getBySourceLead(UUID sourceLeadId) {
        return customerRepository
                .findByTenantIdAndSourceLeadIdAndDeletedAtIsNull(requireTenantId(), sourceLeadId)
                .map(mapper::toDto)
                .orElseThrow(() -> new NotFoundException(
                        "Customer not found for source lead: " + sourceLeadId));
    }

    @Transactional(readOnly = true)
    public PageResponse<CustomerDto> listCustomers(int page, int size, CustomerStatus status, String q) {
        UUID tenantId = requireTenantId();
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 100);
        Page<Customer> result = customerRepository.search(
                tenantId, status, trimToNull(q), PageRequest.of(safePage, safeSize));
        return PageResponse.<CustomerDto>builder()
                .content(result.getContent().stream().map(mapper::toDto).toList())
                .page(result.getNumber())
                .size(result.getSize())
                .totalElements(result.getTotalElements())
                .totalPages(result.getTotalPages())
                .first(result.isFirst())
                .last(result.isLast())
                .build();
    }

    @Transactional
    public CustomerDto updateCustomer(UUID customerId, UpdateCustomerRequest request) {
        Customer customer = requireCustomer(customerId);
        customer.updateDetails(
                request.getFullName(),
                request.getPhone(),
                request.getEmail(),
                request.getStatus(),
                request.getMetadata(),
                actorId());
        Customer saved = customerRepository.save(customer);
        eventPublisher.publish(CustomerUpdatedEvent.of(
                saved.getTenantId().toString(),
                saved.getId().toString(),
                saved.getFullName(),
                saved.getStatus().name(),
                correlationId()));
        return mapper.toDto(saved);
    }

    @Transactional
    public CustomerDto activate(UUID customerId) {
        Customer customer = requireCustomer(customerId);
        customer.activate(actorId());
        Customer saved = customerRepository.save(customer);
        eventPublisher.publish(CustomerUpdatedEvent.of(
                saved.getTenantId().toString(),
                saved.getId().toString(),
                saved.getFullName(),
                saved.getStatus().name(),
                correlationId()));
        return mapper.toDto(saved);
    }

    @Transactional
    public CustomerDto archive(UUID customerId, String reason) {
        Customer customer = requireCustomer(customerId);
        customer.archive(actorId());
        Customer saved = customerRepository.save(customer);
        eventPublisher.publish(CustomerArchivedEvent.of(
                saved.getTenantId().toString(),
                saved.getId().toString(),
                saved.getFullName(),
                StringUtils.hasText(reason) ? reason.trim() : "archived",
                correlationId()));
        return mapper.toDto(saved);
    }

    @Transactional
    public void deleteCustomer(UUID customerId) {
        Customer customer = requireCustomer(customerId);
        customer.softDelete(actorId());
        customerRepository.save(customer);
        eventPublisher.publish(CustomerArchivedEvent.of(
                customer.getTenantId().toString(),
                customer.getId().toString(),
                customer.getFullName(),
                "deleted",
                correlationId()));
    }

    @Transactional
    public CustomerAddressDto addAddress(UUID customerId, CreateCustomerAddressRequest request) {
        Customer customer = requireCustomer(customerId);
        UUID tenantId = customer.getTenantId();
        UUID actor = actorId();
        Instant now = Instant.now();
        boolean primary = Boolean.TRUE.equals(request.getPrimary());

        if (primary) {
            addressRepository.clearPrimaryFlags(tenantId, customerId);
        }

        CustomerAddress address = CustomerAddress.builder()
                .tenantId(tenantId)
                .customerId(customerId)
                .addressType(StringUtils.hasText(request.getAddressType())
                        ? request.getAddressType().trim()
                        : "HOME")
                .line1(request.getLine1().trim())
                .line2(trimToNull(request.getLine2()))
                .city(request.getCity().trim())
                .state(trimToNull(request.getState()))
                .postalCode(trimToNull(request.getPostalCode()))
                .country(StringUtils.hasText(request.getCountry())
                        ? request.getCountry().trim()
                        : "India")
                .primaryAddress(primary)
                .createdAt(now)
                .updatedAt(now)
                .createdBy(actor)
                .updatedBy(actor)
                .build();
        return mapper.toAddressDto(addressRepository.saveAndFlush(address));
    }

    @Transactional(readOnly = true)
    public List<CustomerAddressDto> listAddresses(UUID customerId) {
        requireCustomer(customerId);
        return addressRepository
                .findByTenantIdAndCustomerIdAndDeletedAtIsNullOrderByPrimaryAddressDescCreatedAtAsc(
                        requireTenantId(), customerId)
                .stream()
                .map(mapper::toAddressDto)
                .toList();
    }

    @Transactional
    public CustomerAddressDto updateAddress(
            UUID customerId, UUID addressId, UpdateCustomerAddressRequest request) {
        requireCustomer(customerId);
        CustomerAddress address = addressRepository
                .findByTenantIdAndCustomerIdAndIdAndDeletedAtIsNull(requireTenantId(), customerId, addressId)
                .orElseThrow(() -> new NotFoundException("Address not found: " + addressId));
        address.assertActive();

        if (StringUtils.hasText(request.getAddressType())) {
            address.setAddressType(request.getAddressType().trim());
        }
        if (StringUtils.hasText(request.getLine1())) {
            address.setLine1(request.getLine1().trim());
        }
        if (request.getLine2() != null) {
            address.setLine2(trimToNull(request.getLine2()));
        }
        if (StringUtils.hasText(request.getCity())) {
            address.setCity(request.getCity().trim());
        }
        if (request.getState() != null) {
            address.setState(trimToNull(request.getState()));
        }
        if (request.getPostalCode() != null) {
            address.setPostalCode(trimToNull(request.getPostalCode()));
        }
        if (StringUtils.hasText(request.getCountry())) {
            address.setCountry(request.getCountry().trim());
        }
        if (Boolean.TRUE.equals(request.getPrimary())) {
            addressRepository.clearPrimaryFlags(requireTenantId(), customerId);
            address.setPrimaryAddress(true);
        } else if (Boolean.FALSE.equals(request.getPrimary())) {
            address.setPrimaryAddress(false);
        }
        address.setUpdatedAt(Instant.now());
        address.setUpdatedBy(actorId());
        return mapper.toAddressDto(addressRepository.save(address));
    }

    @Transactional
    public void deleteAddress(UUID customerId, UUID addressId) {
        requireCustomer(customerId);
        CustomerAddress address = addressRepository
                .findByTenantIdAndCustomerIdAndIdAndDeletedAtIsNull(requireTenantId(), customerId, addressId)
                .orElseThrow(() -> new NotFoundException("Address not found: " + addressId));
        address.softDelete(actorId());
        addressRepository.save(address);
    }

    private Customer requireCustomer(UUID customerId) {
        return customerRepository.findByTenantIdAndIdAndDeletedAtIsNull(requireTenantId(), customerId)
                .orElseThrow(() -> new NotFoundException("Customer not found: " + customerId));
    }

    private UUID requireTenantId() {
        String raw = TenantContext.getTenantId();
        if (!StringUtils.hasText(raw)) {
            throw new ValidationException("Tenant context is required");
        }
        return UUID.fromString(raw);
    }

    private UUID actorId() {
        return parseUuidOrNull(TenantContext.getUserId());
    }

    private String correlationId() {
        return CorrelationIdUtils.get().orElseGet(CorrelationIdUtils::generate);
    }

    private static UUID parseUuidOrNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private static String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }
}
