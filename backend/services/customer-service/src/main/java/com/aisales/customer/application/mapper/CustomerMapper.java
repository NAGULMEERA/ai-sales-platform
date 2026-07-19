package com.aisales.customer.application.mapper;

import com.aisales.common.contracts.customer.CustomerAddressDto;
import com.aisales.common.contracts.customer.CustomerConsentDto;
import com.aisales.common.contracts.customer.CustomerContactMethodDto;
import com.aisales.common.contracts.customer.CustomerDto;
import com.aisales.common.contracts.customer.CustomerDuplicateDto;
import com.aisales.common.contracts.customer.CustomerInteractionDto;
import com.aisales.common.contracts.customer.CustomerTimelineEntryDto;
import com.aisales.customer.domain.entity.Customer;
import com.aisales.customer.domain.entity.CustomerAddress;
import com.aisales.customer.domain.entity.CustomerConsent;
import com.aisales.customer.domain.entity.CustomerContactMethod;
import com.aisales.customer.domain.entity.CustomerDuplicate;
import com.aisales.customer.domain.entity.CustomerInteraction;
import com.aisales.customer.domain.entity.CustomerTimelineEntry;
import java.util.HashMap;
import org.springframework.stereotype.Component;

@Component
public class CustomerMapper {

    public CustomerDto toDto(Customer customer) {
        return CustomerDto.builder()
                .id(customer.getId())
                .tenantId(customer.getTenantId())
                .organizationId(customer.getOrganizationId())
                .customerNumber(customer.getCustomerNumber())
                .fullName(customer.getFullName())
                .phone(customer.getPhone())
                .email(customer.getEmail())
                .whatsapp(customer.getWhatsapp())
                .externalCrmId(customer.getExternalCrmId())
                .governmentId(customer.getGovernmentId())
                .gender(customer.getGender())
                .dateOfBirth(customer.getDateOfBirth())
                .language(customer.getLanguage())
                .preferredChannel(customer.getPreferredChannel())
                .status(customer.getStatus())
                .sourceType(customer.getSourceType())
                .sourceLeadId(customer.getSourceLeadId())
                .mergedIntoCustomerId(customer.getMergedIntoCustomerId())
                .preferences(customer.getPreferences() == null
                        ? new HashMap<>()
                        : new HashMap<>(customer.getPreferences()))
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

    public CustomerContactMethodDto toContactDto(CustomerContactMethod method) {
        return CustomerContactMethodDto.builder()
                .id(method.getId())
                .customerId(method.getCustomerId())
                .methodType(method.getMethodType())
                .value(method.getValue())
                .label(method.getLabel())
                .verified(method.isVerified())
                .verifiedAt(method.getVerifiedAt())
                .primary(method.isPrimaryMethod())
                .createdAt(method.getCreatedAt())
                .build();
    }

    public CustomerConsentDto toConsentDto(CustomerConsent consent) {
        return CustomerConsentDto.builder()
                .id(consent.getId())
                .customerId(consent.getCustomerId())
                .consentType(consent.getConsentType())
                .consentVersion(consent.getConsentVersion())
                .granted(consent.isGranted())
                .grantedAt(consent.getGrantedAt())
                .withdrawnAt(consent.getWithdrawnAt())
                .source(consent.getSource())
                .build();
    }

    public CustomerTimelineEntryDto toTimelineDto(CustomerTimelineEntry entry) {
        return CustomerTimelineEntryDto.builder()
                .id(entry.getId())
                .customerId(entry.getCustomerId())
                .eventType(entry.getEventType())
                .summary(entry.getSummary())
                .eventData(entry.getEventData() == null
                        ? new HashMap<>()
                        : new HashMap<>(entry.getEventData()))
                .occurredAt(entry.getOccurredAt())
                .createdBy(entry.getCreatedBy())
                .build();
    }

    public CustomerInteractionDto toInteractionDto(CustomerInteraction interaction) {
        return CustomerInteractionDto.builder()
                .id(interaction.getId())
                .customerId(interaction.getCustomerId())
                .interactionType(interaction.getInteractionType())
                .channel(interaction.getChannel())
                .summary(interaction.getSummary())
                .details(interaction.getDetails() == null
                        ? new HashMap<>()
                        : new HashMap<>(interaction.getDetails()))
                .occurredAt(interaction.getOccurredAt())
                .build();
    }

    public CustomerDuplicateDto toDuplicateDto(CustomerDuplicate duplicate) {
        return CustomerDuplicateDto.builder()
                .id(duplicate.getId())
                .customerId(duplicate.getCustomerId())
                .duplicateOfCustomerId(duplicate.getDuplicateOfCustomerId())
                .matchReasons(duplicate.getMatchReasons())
                .similarityScore(duplicate.getSimilarityScore())
                .matchStrength(duplicate.getMatchStrength())
                .detectedAt(duplicate.getDetectedAt())
                .resolved(duplicate.isResolved())
                .mergedIntoCustomerId(duplicate.getMergedIntoCustomerId())
                .build();
    }
}
