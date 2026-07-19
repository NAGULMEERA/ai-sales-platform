package com.aisales.customer.application.service;

import com.aisales.common.contracts.customer.AddContactMethodRequest;
import com.aisales.common.contracts.customer.ContactMethodType;
import com.aisales.common.contracts.customer.ConvertLeadToCustomerRequest;
import com.aisales.common.contracts.customer.CreateCustomerAddressRequest;
import com.aisales.common.contracts.customer.CreateCustomerRequest;
import com.aisales.common.contracts.customer.CustomerAddressDto;
import com.aisales.common.contracts.customer.CustomerConsentDto;
import com.aisales.common.contracts.customer.CustomerContactMethodDto;
import com.aisales.common.contracts.customer.CustomerDto;
import com.aisales.common.contracts.customer.CustomerDuplicateDto;
import com.aisales.common.contracts.customer.CustomerInteractionDto;
import com.aisales.common.contracts.customer.CustomerSourceType;
import com.aisales.common.contracts.customer.CustomerStatus;
import com.aisales.common.contracts.customer.CustomerTimelineEntryDto;
import com.aisales.common.contracts.customer.IdentityMatchType;
import com.aisales.common.contracts.customer.IdentityResolutionResultDto;
import com.aisales.common.contracts.customer.LinkLeadRequest;
import com.aisales.common.contracts.customer.MergeCustomerRequest;
import com.aisales.common.contracts.customer.RecordConsentRequest;
import com.aisales.common.contracts.customer.RecordInteractionRequest;
import com.aisales.common.contracts.customer.ResolveIdentityRequest;
import com.aisales.common.contracts.customer.UpdateCustomerAddressRequest;
import com.aisales.common.contracts.customer.UpdateCustomerRequest;
import com.aisales.common.contracts.customer.UpdatePreferencesRequest;
import com.aisales.common.core.dto.PageResponse;
import com.aisales.common.core.util.CorrelationIdUtils;
import com.aisales.common.core.util.TenantContext;
import com.aisales.common.events.model.CustomerArchivedEvent;
import com.aisales.common.events.model.CustomerCreatedEvent;
import com.aisales.common.events.model.CustomerDeactivatedEvent;
import com.aisales.common.events.model.CustomerMergedEvent;
import com.aisales.common.events.model.CustomerUpdatedEvent;
import com.aisales.common.events.model.CustomerVerifiedEvent;
import com.aisales.common.events.model.LeadConvertedToCustomerEvent;
import com.aisales.common.events.publisher.EventPublisher;
import com.aisales.common.exception.exception.NotFoundException;
import com.aisales.common.exception.exception.ValidationException;
import com.aisales.common.observability.metrics.MetricNames;
import com.aisales.common.observability.metrics.PlatformMetrics;
import com.aisales.customer.application.mapper.CustomerMapper;
import com.aisales.customer.domain.entity.Customer;
import com.aisales.customer.domain.entity.CustomerAddress;
import com.aisales.customer.domain.entity.CustomerConsent;
import com.aisales.customer.domain.entity.CustomerContactMethod;
import com.aisales.customer.domain.entity.CustomerDuplicate;
import com.aisales.customer.domain.entity.CustomerInteraction;
import com.aisales.customer.infrastructure.persistence.CustomerAddressRepository;
import com.aisales.customer.infrastructure.persistence.CustomerConsentRepository;
import com.aisales.customer.infrastructure.persistence.CustomerContactMethodRepository;
import com.aisales.customer.infrastructure.persistence.CustomerDuplicateRepository;
import com.aisales.customer.infrastructure.persistence.CustomerInteractionRepository;
import com.aisales.customer.infrastructure.persistence.CustomerRepository;
import com.aisales.customer.infrastructure.persistence.CustomerTimelineRepository;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
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
    private final CustomerContactMethodRepository contactMethodRepository;
    private final CustomerConsentRepository consentRepository;
    private final CustomerTimelineRepository timelineRepository;
    private final CustomerInteractionRepository interactionRepository;
    private final CustomerDuplicateRepository duplicateRepository;
    private final CustomerMapper mapper;
    private final EventPublisher eventPublisher;
    private final CustomerTimelineRecorder timelineRecorder;
    private final CustomerDuplicateDetectionService duplicateDetection;
    private final CustomerIdentityResolutionService identityResolutionService;
    private final CustomerIdempotencyService idempotencyService;
    private final ObjectProvider<PlatformMetrics> platformMetrics;

    @Transactional
    public CustomerDto createCustomer(CreateCustomerRequest request) {
        return createCustomer(request, null);
    }

    @Transactional
    public CustomerDto createCustomer(CreateCustomerRequest request, String idempotencyKey) {
        var cached = idempotencyService.beginCreate(idempotencyKey);
        if (cached.isPresent()) {
            return cached.get();
        }

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
                .whatsapp(trimToNull(request.getWhatsapp()))
                .externalCrmId(trimToNull(request.getExternalCrmId()))
                .governmentId(trimToNull(request.getGovernmentId()))
                .gender(trimToNull(request.getGender()))
                .dateOfBirth(request.getDateOfBirth())
                .language(trimToNull(request.getLanguage()))
                .preferredChannel(trimToNull(request.getPreferredChannel()))
                .status(CustomerStatus.PROSPECT)
                .sourceType(sourceType)
                .sourceLeadId(request.getSourceLeadId())
                .preferences(request.getPreferences() != null
                        ? new HashMap<>(request.getPreferences())
                        : new HashMap<>())
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
        seedPrimaryContacts(saved, actor, now);
        duplicateDetection.detectAndRecord(saved);
        timelineRecorder.record(tenantId, saved.getId(), "CUSTOMER_CREATED", "Customer created", actor);

        eventPublisher.publish(CustomerCreatedEvent.of(
                tenantId.toString(),
                saved.getId().toString(),
                saved.getFullName(),
                saved.getEmail(),
                correlationId()));
        incrementMetric(MetricNames.CUSTOMER_CREATED, tenantId);

        CustomerDto dto = mapper.toDto(saved);
        idempotencyService.storeCreateResponse(idempotencyKey, saved.getId(), dto);
        return dto;
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
        UUID actor = actorId();
        customer.updateProfile(
                request.getFullName(),
                request.getGender(),
                request.getDateOfBirth(),
                request.getLanguage(),
                request.getMetadata(),
                actor);
        customer.updateContactDetails(
                request.getPhone(),
                request.getEmail(),
                request.getWhatsapp(),
                request.getExternalCrmId(),
                request.getGovernmentId(),
                actor);
        if (request.getPreferredChannel() != null || request.getPreferences() != null) {
            customer.updatePreferences(
                    request.getPreferredChannel(), request.getLanguage(), request.getPreferences(), actor);
        }
        if (request.getStatus() != null) {
            if (customer.getStatus() == CustomerStatus.ARCHIVED
                    && request.getStatus() != CustomerStatus.ARCHIVED) {
                throw new ValidationException("Archived customer cannot change status without restore flow");
            }
            customer.setStatus(request.getStatus());
            customer.setUpdatedAt(Instant.now());
            customer.setUpdatedBy(actor);
        }
        Customer saved = customerRepository.save(customer);
        duplicateDetection.detectAndRecord(saved);
        timelineRecorder.record(
                saved.getTenantId(), saved.getId(), "PROFILE_UPDATED", "Customer profile updated", actor);
        eventPublisher.publish(CustomerUpdatedEvent.of(
                saved.getTenantId().toString(),
                saved.getId().toString(),
                saved.getFullName(),
                saved.getStatus().name(),
                correlationId()));
        incrementMetric(MetricNames.CUSTOMER_UPDATED, saved.getTenantId());
        return mapper.toDto(saved);
    }

    @Transactional
    public CustomerDto updatePreferences(UUID customerId, UpdatePreferencesRequest request) {
        Customer customer = requireCustomer(customerId);
        UUID actor = actorId();
        customer.updatePreferences(
                request.getPreferredChannel(), request.getLanguage(), request.getPreferences(), actor);
        Customer saved = customerRepository.save(customer);
        timelineRecorder.record(
                saved.getTenantId(), saved.getId(), "PREFERENCES_CHANGED", "Preferences updated", actor);
        eventPublisher.publish(CustomerUpdatedEvent.of(
                saved.getTenantId().toString(),
                saved.getId().toString(),
                saved.getFullName(),
                saved.getStatus().name(),
                correlationId()));
        incrementMetric(MetricNames.CUSTOMER_UPDATED, saved.getTenantId());
        return mapper.toDto(saved);
    }

    @Transactional
    public CustomerDto activate(UUID customerId) {
        Customer customer = requireCustomer(customerId);
        customer.activate(actorId());
        Customer saved = customerRepository.save(customer);
        timelineRecorder.record(
                saved.getTenantId(), saved.getId(), "CUSTOMER_ACTIVATED", "Customer activated", actorId());
        eventPublisher.publish(CustomerUpdatedEvent.of(
                saved.getTenantId().toString(),
                saved.getId().toString(),
                saved.getFullName(),
                saved.getStatus().name(),
                correlationId()));
        return mapper.toDto(saved);
    }

    @Transactional
    public CustomerDto deactivate(UUID customerId, String reason) {
        Customer customer = requireCustomer(customerId);
        UUID actor = actorId();
        customer.deactivate(actor);
        Customer saved = customerRepository.save(customer);
        timelineRecorder.record(
                saved.getTenantId(), saved.getId(), "CUSTOMER_DEACTIVATED",
                StringUtils.hasText(reason) ? reason.trim() : "deactivated", actor);
        eventPublisher.publish(CustomerDeactivatedEvent.of(
                saved.getTenantId().toString(),
                saved.getId().toString(),
                saved.getFullName(),
                StringUtils.hasText(reason) ? reason.trim() : "deactivated",
                correlationId()));
        incrementMetric(MetricNames.CUSTOMER_DEACTIVATED, saved.getTenantId());
        return mapper.toDto(saved);
    }

    @Transactional
    public CustomerDto reactivate(UUID customerId) {
        Customer customer = requireCustomer(customerId);
        UUID actor = actorId();
        customer.reactivate(actor);
        Customer saved = customerRepository.save(customer);
        timelineRecorder.record(
                saved.getTenantId(), saved.getId(), "CUSTOMER_REACTIVATED", "Customer reactivated", actor);
        eventPublisher.publish(CustomerUpdatedEvent.of(
                saved.getTenantId().toString(),
                saved.getId().toString(),
                saved.getFullName(),
                saved.getStatus().name(),
                correlationId()));
        incrementMetric(MetricNames.CUSTOMER_REACTIVATED, saved.getTenantId());
        return mapper.toDto(saved);
    }

    @Transactional
    public CustomerDto archive(UUID customerId, String reason) {
        Customer customer = requireCustomer(customerId);
        UUID actor = actorId();
        customer.archive(actor);
        Customer saved = customerRepository.save(customer);
        timelineRecorder.record(
                saved.getTenantId(), saved.getId(), "CUSTOMER_ARCHIVED",
                StringUtils.hasText(reason) ? reason.trim() : "archived", actor);
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
        UUID actor = actorId();
        customer.softDelete(actor);
        customerRepository.save(customer);
        timelineRecorder.record(
                customer.getTenantId(), customer.getId(), "CUSTOMER_DELETED", "Customer soft-deleted", actor);
        eventPublisher.publish(CustomerArchivedEvent.of(
                customer.getTenantId().toString(),
                customer.getId().toString(),
                customer.getFullName(),
                "deleted",
                correlationId()));
    }

    @Transactional
    public CustomerDto convertLeadToCustomer(ConvertLeadToCustomerRequest request) {
        UUID tenantId = requireTenantId();
        UUID actor = actorId();

        var existingForLead = customerRepository.findByTenantIdAndSourceLeadIdAndDeletedAtIsNull(
                tenantId, request.getLeadId());
        if (existingForLead.isPresent()) {
            return mapper.toDto(existingForLead.get());
        }

        if (request.getExistingCustomerId() != null) {
            Customer linked = requireCustomer(request.getExistingCustomerId());
            linked.linkLead(request.getLeadId(), actor);
            Customer saved = customerRepository.save(linked);
            timelineRecorder.record(
                    tenantId, saved.getId(), "LEAD_LINKED",
                    "Lead linked to existing customer", Map.of("leadId", request.getLeadId().toString()), actor);
            publishLeadConverted(saved, request.getLeadId(), true);
            return mapper.toDto(saved);
        }

        IdentityResolutionResultDto identity = identityResolutionService.resolve(ResolveIdentityRequest.builder()
                .phone(request.getPhone())
                .email(request.getEmail())
                .whatsapp(request.getWhatsapp())
                .build());

        if (identity.getMatchType() == IdentityMatchType.EXACT_MATCH && !identity.getCandidates().isEmpty()) {
            Customer linked = requireCustomer(identity.getCandidates().get(0).getCustomerId());
            linked.linkLead(request.getLeadId(), actor);
            if (StringUtils.hasText(request.getFullName()) && !StringUtils.hasText(linked.getFullName())) {
                linked.updateProfile(request.getFullName(), null, null, null, null, actor);
            }
            Customer saved = customerRepository.save(linked);
            timelineRecorder.record(
                    tenantId, saved.getId(), "LEAD_CONVERTED",
                    "Lead linked via identity resolution", Map.of("leadId", request.getLeadId().toString()), actor);
            publishLeadConverted(saved, request.getLeadId(), true);
            return mapper.toDto(saved);
        }

        CustomerDto created = createCustomer(CreateCustomerRequest.builder()
                .fullName(request.getFullName())
                .phone(request.getPhone())
                .email(request.getEmail())
                .whatsapp(request.getWhatsapp())
                .sourceType(CustomerSourceType.LEAD_CONVERSION)
                .sourceLeadId(request.getLeadId())
                .metadata(request.getMetadata())
                .build());
        Customer saved = requireCustomer(created.getId());
        timelineRecorder.record(
                tenantId, saved.getId(), "LEAD_CONVERTED",
                "Lead converted to new customer", Map.of("leadId", request.getLeadId().toString()), actor);
        publishLeadConverted(saved, request.getLeadId(), false);
        return created;
    }

    @Transactional
    public CustomerDto linkLead(UUID customerId, LinkLeadRequest request) {
        Customer customer = requireCustomer(customerId);
        UUID actor = actorId();
        UUID tenantId = customer.getTenantId();
        customerRepository.findByTenantIdAndSourceLeadIdAndDeletedAtIsNull(tenantId, request.getLeadId())
                .ifPresent(existing -> {
                    if (!existing.getId().equals(customerId)) {
                        throw new ValidationException(
                                "Lead already linked to customer: " + existing.getId());
                    }
                });
        customer.linkLead(request.getLeadId(), actor);
        Customer saved = customerRepository.save(customer);
        timelineRecorder.record(
                tenantId, saved.getId(), "LEAD_LINKED",
                "Lead linked", Map.of("leadId", request.getLeadId().toString()), actor);
        publishLeadConverted(saved, request.getLeadId(), true);
        return mapper.toDto(saved);
    }

    @Transactional
    public CustomerDto mergeCustomer(UUID survivorId, MergeCustomerRequest request) {
        if (survivorId.equals(request.getLoserCustomerId())) {
            throw new ValidationException("Cannot merge a customer into itself");
        }
        Customer survivor = requireCustomer(survivorId);
        Customer loser = customerRepository
                .findByTenantIdAndIdAndDeletedAtIsNull(requireTenantId(), request.getLoserCustomerId())
                .orElseThrow(() -> new NotFoundException(
                        "Customer not found: " + request.getLoserCustomerId()));

        UUID actor = actorId();
        UUID tenantId = survivor.getTenantId();

        survivor.absorbFrom(loser, actor);
        customerRepository.save(survivor);

        addressRepository.reassignToSurvivor(tenantId, loser.getId(), survivor.getId());
        contactMethodRepository.reassignToSurvivor(tenantId, loser.getId(), survivor.getId());
        consentRepository.reassignToSurvivor(tenantId, loser.getId(), survivor.getId());
        timelineRepository.reassignToSurvivor(tenantId, loser.getId(), survivor.getId());
        interactionRepository.reassignToSurvivor(tenantId, loser.getId(), survivor.getId());

        loser.markMergedInto(survivor.getId(), actor);
        customerRepository.save(loser);

        Instant now = Instant.now();
        duplicateRepository.findByTenantIdAndCustomerIdAndResolvedFalse(tenantId, survivorId).forEach(dup -> {
            if (dup.getDuplicateOfCustomerId().equals(loser.getId())) {
                dup.setResolved(true);
                dup.setResolvedAt(now);
                dup.setMergedIntoCustomerId(survivorId);
                duplicateRepository.save(dup);
            }
        });
        duplicateRepository.findByTenantIdAndCustomerIdAndResolvedFalse(tenantId, loser.getId()).forEach(dup -> {
            dup.setResolved(true);
            dup.setResolvedAt(now);
            dup.setMergedIntoCustomerId(survivorId);
            duplicateRepository.save(dup);
        });

        timelineRecorder.record(
                tenantId, survivor.getId(), "CUSTOMER_MERGED",
                StringUtils.hasText(request.getReason()) ? request.getReason().trim() : "Customers merged",
                Map.of("mergedCustomerId", loser.getId().toString()),
                actor);

        eventPublisher.publish(CustomerMergedEvent.of(
                tenantId.toString(),
                survivor.getId().toString(),
                survivor.getFullName(),
                loser.getId().toString(),
                correlationId()));
        incrementMetric(MetricNames.CUSTOMER_MERGED, tenantId);
        return mapper.toDto(survivor);
    }

    @Transactional(readOnly = true)
    public IdentityResolutionResultDto resolveIdentity(ResolveIdentityRequest request) {
        return identityResolutionService.resolve(request);
    }

    @Transactional(readOnly = true)
    public List<CustomerDuplicateDto> listDuplicates(UUID customerId) {
        requireCustomer(customerId);
        return duplicateRepository
                .findByTenantIdAndCustomerIdAndResolvedFalse(requireTenantId(), customerId)
                .stream()
                .map(mapper::toDuplicateDto)
                .toList();
    }

    @Transactional
    public CustomerDuplicateDto resolveDuplicate(UUID customerId, UUID duplicateId, boolean merge) {
        requireCustomer(customerId);
        CustomerDuplicate dup = duplicateRepository
                .findByTenantIdAndId(requireTenantId(), duplicateId)
                .orElseThrow(() -> new NotFoundException("Duplicate not found: " + duplicateId));
        if (!dup.getCustomerId().equals(customerId)
                && !dup.getDuplicateOfCustomerId().equals(customerId)) {
            throw new ValidationException("Duplicate does not belong to this customer");
        }
        if (merge) {
            UUID survivorId = customerId;
            UUID loserId = dup.getCustomerId().equals(customerId)
                    ? dup.getDuplicateOfCustomerId()
                    : dup.getCustomerId();
            mergeCustomer(survivorId, MergeCustomerRequest.builder().loserCustomerId(loserId).build());
            return mapper.toDuplicateDto(duplicateRepository.findByTenantIdAndId(requireTenantId(), duplicateId)
                    .orElse(dup));
        }
        dup.setResolved(true);
        dup.setResolvedAt(Instant.now());
        return mapper.toDuplicateDto(duplicateRepository.save(dup));
    }

    @Transactional
    public CustomerContactMethodDto addContactMethod(UUID customerId, AddContactMethodRequest request) {
        Customer customer = requireCustomer(customerId);
        UUID tenantId = customer.getTenantId();
        UUID actor = actorId();
        Instant now = Instant.now();
        boolean primary = Boolean.TRUE.equals(request.getPrimary());
        if (primary) {
            contactMethodRepository.clearPrimaryFlags(tenantId, customerId, request.getMethodType());
            customer.syncPrimaryContact(request.getMethodType().name(), request.getValue(), actor);
            customerRepository.save(customer);
        }
        CustomerContactMethod method = contactMethodRepository.saveAndFlush(CustomerContactMethod.builder()
                .tenantId(tenantId)
                .customerId(customerId)
                .methodType(request.getMethodType())
                .value(request.getValue().trim())
                .label(trimToNull(request.getLabel()))
                .primaryMethod(primary)
                .createdAt(now)
                .updatedAt(now)
                .createdBy(actor)
                .updatedBy(actor)
                .build());
        timelineRecorder.record(
                tenantId, customerId, "CONTACT_ADDED",
                request.getMethodType().name() + " added", actor);
        return mapper.toContactDto(method);
    }

    @Transactional(readOnly = true)
    public List<CustomerContactMethodDto> listContactMethods(UUID customerId) {
        requireCustomer(customerId);
        return contactMethodRepository
                .findByTenantIdAndCustomerIdAndDeletedAtIsNullOrderByPrimaryMethodDescCreatedAtAsc(
                        requireTenantId(), customerId)
                .stream()
                .map(mapper::toContactDto)
                .toList();
    }

    @Transactional
    public CustomerContactMethodDto verifyContact(UUID customerId, UUID contactId) {
        Customer customer = requireCustomer(customerId);
        CustomerContactMethod method = contactMethodRepository
                .findByTenantIdAndCustomerIdAndIdAndDeletedAtIsNull(
                        requireTenantId(), customerId, contactId)
                .orElseThrow(() -> new NotFoundException("Contact method not found: " + contactId));
        UUID actor = actorId();
        method.verify(actor);
        contactMethodRepository.save(method);
        timelineRecorder.record(
                customer.getTenantId(), customerId, "CONTACT_VERIFIED",
                method.getMethodType().name() + " verified", actor);
        eventPublisher.publish(CustomerVerifiedEvent.of(
                customer.getTenantId().toString(),
                customerId.toString(),
                customer.getFullName(),
                method.getMethodType().name(),
                method.getValue(),
                correlationId()));
        incrementMetric(MetricNames.CUSTOMER_VERIFIED, customer.getTenantId());
        return mapper.toContactDto(method);
    }

    @Transactional
    public CustomerConsentDto recordConsent(UUID customerId, RecordConsentRequest request) {
        Customer customer = requireCustomer(customerId);
        UUID tenantId = customer.getTenantId();
        UUID actor = actorId();
        String version = StringUtils.hasText(request.getConsentVersion())
                ? request.getConsentVersion().trim()
                : "1";
        boolean granted = request.getGranted() == null || request.getGranted();

        CustomerConsent consent = consentRepository
                .findByTenantIdAndCustomerIdAndConsentTypeAndConsentVersion(
                        tenantId, customerId, request.getConsentType().trim(), version)
                .orElseGet(() -> CustomerConsent.builder()
                        .tenantId(tenantId)
                        .customerId(customerId)
                        .consentType(request.getConsentType().trim())
                        .consentVersion(version)
                        .grantedAt(Instant.now())
                        .createdBy(actor)
                        .build());

        if (granted) {
            consent.setGranted(true);
            consent.setGrantedAt(Instant.now());
            consent.setWithdrawnAt(null);
        } else {
            consent.withdraw();
        }
        consent.setSource(trimToNull(request.getSource()));
        CustomerConsent saved = consentRepository.save(consent);
        timelineRecorder.record(
                tenantId, customerId, "CONSENT_CHANGED",
                saved.getConsentType() + (granted ? " granted" : " withdrawn"), actor);
        return mapper.toConsentDto(saved);
    }

    @Transactional(readOnly = true)
    public List<CustomerConsentDto> listConsents(UUID customerId) {
        requireCustomer(customerId);
        return consentRepository
                .findByTenantIdAndCustomerIdOrderByGrantedAtDesc(requireTenantId(), customerId)
                .stream()
                .map(mapper::toConsentDto)
                .toList();
    }

    @Transactional
    public CustomerInteractionDto recordInteraction(UUID customerId, RecordInteractionRequest request) {
        Customer customer = requireCustomer(customerId);
        UUID actor = actorId();
        CustomerInteraction interaction = interactionRepository.save(CustomerInteraction.builder()
                .tenantId(customer.getTenantId())
                .customerId(customerId)
                .interactionType(request.getInteractionType().trim())
                .channel(request.getChannel().trim())
                .summary(trimToNull(request.getSummary()))
                .details(request.getDetails() != null ? new HashMap<>(request.getDetails()) : new HashMap<>())
                .occurredAt(Instant.now())
                .createdBy(actor)
                .build());
        timelineRecorder.record(
                customer.getTenantId(),
                customerId,
                "INTERACTION_RECORDED",
                request.getInteractionType().trim(),
                Map.of("channel", request.getChannel().trim()),
                actor);
        return mapper.toInteractionDto(interaction);
    }

    @Transactional(readOnly = true)
    public List<CustomerInteractionDto> listInteractions(UUID customerId) {
        requireCustomer(customerId);
        return interactionRepository
                .findByTenantIdAndCustomerIdOrderByOccurredAtDesc(requireTenantId(), customerId)
                .stream()
                .map(mapper::toInteractionDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<CustomerTimelineEntryDto> timeline(UUID customerId) {
        requireCustomer(customerId);
        return timelineRepository
                .findByTenantIdAndCustomerIdOrderByOccurredAtDesc(requireTenantId(), customerId)
                .stream()
                .map(mapper::toTimelineDto)
                .toList();
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
        CustomerAddressDto dto = mapper.toAddressDto(addressRepository.saveAndFlush(address));
        timelineRecorder.record(tenantId, customerId, "ADDRESS_ADDED", "Address added", actor);
        return dto;
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

    private void seedPrimaryContacts(Customer customer, UUID actor, Instant now) {
        if (StringUtils.hasText(customer.getPhone())) {
            contactMethodRepository.save(CustomerContactMethod.builder()
                    .tenantId(customer.getTenantId())
                    .customerId(customer.getId())
                    .methodType(ContactMethodType.PHONE)
                    .value(customer.getPhone())
                    .primaryMethod(true)
                    .createdAt(now)
                    .updatedAt(now)
                    .createdBy(actor)
                    .updatedBy(actor)
                    .build());
        }
        if (StringUtils.hasText(customer.getEmail())) {
            contactMethodRepository.save(CustomerContactMethod.builder()
                    .tenantId(customer.getTenantId())
                    .customerId(customer.getId())
                    .methodType(ContactMethodType.EMAIL)
                    .value(customer.getEmail())
                    .primaryMethod(true)
                    .createdAt(now)
                    .updatedAt(now)
                    .createdBy(actor)
                    .updatedBy(actor)
                    .build());
        }
        if (StringUtils.hasText(customer.getWhatsapp())) {
            contactMethodRepository.save(CustomerContactMethod.builder()
                    .tenantId(customer.getTenantId())
                    .customerId(customer.getId())
                    .methodType(ContactMethodType.WHATSAPP)
                    .value(customer.getWhatsapp())
                    .primaryMethod(true)
                    .createdAt(now)
                    .updatedAt(now)
                    .createdBy(actor)
                    .updatedBy(actor)
                    .build());
        }
    }

    private void publishLeadConverted(Customer customer, UUID leadId, boolean linkedExisting) {
        eventPublisher.publish(LeadConvertedToCustomerEvent.of(
                customer.getTenantId().toString(),
                customer.getId().toString(),
                customer.getFullName(),
                leadId.toString(),
                linkedExisting,
                correlationId()));
        incrementMetric(MetricNames.CUSTOMER_CONVERTED, customer.getTenantId());
    }

    private void incrementMetric(String name, UUID tenantId) {
        PlatformMetrics metrics = platformMetrics.getIfAvailable();
        if (metrics != null && tenantId != null) {
            metrics.incrementForTenant(name, tenantId.toString());
        }
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
