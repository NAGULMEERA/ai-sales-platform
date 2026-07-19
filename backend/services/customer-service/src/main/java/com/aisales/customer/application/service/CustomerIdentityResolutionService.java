package com.aisales.customer.application.service;

import com.aisales.common.contracts.customer.IdentityMatchCandidateDto;
import com.aisales.common.contracts.customer.IdentityMatchType;
import com.aisales.common.contracts.customer.IdentityResolutionResultDto;
import com.aisales.common.contracts.customer.ResolveIdentityRequest;
import com.aisales.common.core.util.TenantContext;
import com.aisales.common.exception.exception.ValidationException;
import com.aisales.customer.config.CustomerMatchingProperties;
import com.aisales.customer.domain.entity.Customer;
import com.aisales.customer.infrastructure.persistence.CustomerRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class CustomerIdentityResolutionService {

    private final CustomerRepository customerRepository;
    private final CustomerMatchingProperties matchingProperties;

    @Transactional(readOnly = true)
    public IdentityResolutionResultDto resolve(ResolveIdentityRequest request) {
        UUID tenantId = requireTenantId();
        String phone = matchingProperties.isMatchPhone() ? trim(request.getPhone()) : null;
        String email = matchingProperties.isMatchEmail() ? trim(request.getEmail()) : null;
        String whatsapp = matchingProperties.isMatchWhatsapp() ? trim(request.getWhatsapp()) : null;
        String externalCrmId =
                matchingProperties.isMatchExternalCrmId() ? trim(request.getExternalCrmId()) : null;
        String governmentId =
                matchingProperties.isMatchGovernmentId() ? trim(request.getGovernmentId()) : null;
        String customerNumber = trim(request.getCustomerNumber());

        if (phone == null && email == null && whatsapp == null
                && externalCrmId == null && governmentId == null && customerNumber == null) {
            throw new ValidationException("At least one identity attribute is required");
        }

        List<Customer> candidates = customerRepository.findIdentityCandidates(
                tenantId, phone, email, whatsapp, externalCrmId, governmentId, customerNumber);

        List<IdentityMatchCandidateDto> scored = new ArrayList<>();
        for (Customer customer : candidates) {
            List<String> matchedOn = new ArrayList<>();
            int exactSignals = 0;
            int weakSignals = 0;

            if (phone != null && phone.equals(customer.getPhone())) {
                matchedOn.add("PHONE");
                exactSignals++;
            }
            if (email != null && customer.getEmail() != null
                    && email.equalsIgnoreCase(customer.getEmail())) {
                matchedOn.add("EMAIL");
                exactSignals++;
            }
            if (whatsapp != null && whatsapp.equals(customer.getWhatsapp())) {
                matchedOn.add("WHATSAPP");
                exactSignals++;
            }
            if (externalCrmId != null && externalCrmId.equals(customer.getExternalCrmId())) {
                matchedOn.add("EXTERNAL_CRM_ID");
                exactSignals++;
            }
            if (governmentId != null && governmentId.equals(customer.getGovernmentId())) {
                matchedOn.add("GOVERNMENT_ID");
                exactSignals++;
            }
            if (customerNumber != null && customerNumber.equalsIgnoreCase(customer.getCustomerNumber())) {
                matchedOn.add("CUSTOMER_NUMBER");
                exactSignals++;
            }

            // Name-like weak signal is not used for identity resolution without name input.
            if (matchedOn.isEmpty()) {
                continue;
            }

            IdentityMatchType type = exactSignals > 0
                    ? IdentityMatchType.EXACT_MATCH
                    : (weakSignals >= matchingProperties.getWeakMatchThreshold()
                            ? IdentityMatchType.PROBABLE_MATCH
                            : IdentityMatchType.PROBABLE_MATCH);

            // Single non-strong attribute (only one matched weak) still probable when exactSignals==0;
            // any configured identity hit is at least PROBABLE; strong IDs are EXACT.
            if (exactSignals == 0) {
                type = IdentityMatchType.PROBABLE_MATCH;
            } else if (exactSignals == 1
                    && matchedOn.size() == 1
                    && ("PHONE".equals(matchedOn.get(0)) || "EMAIL".equals(matchedOn.get(0))
                    || "WHATSAPP".equals(matchedOn.get(0)))) {
                // One strong contact match → EXACT for identity resolution
                type = IdentityMatchType.EXACT_MATCH;
            }

            BigDecimal score = BigDecimal.valueOf(Math.min(1.0, 0.4 + (0.2 * exactSignals) + (0.1 * weakSignals)))
                    .setScale(4, RoundingMode.HALF_UP);

            scored.add(IdentityMatchCandidateDto.builder()
                    .customerId(customer.getId())
                    .fullName(customer.getFullName())
                    .phone(customer.getPhone())
                    .email(customer.getEmail())
                    .whatsapp(customer.getWhatsapp())
                    .customerNumber(customer.getCustomerNumber())
                    .matchType(type)
                    .score(score)
                    .matchedOn(matchedOn)
                    .build());
        }

        scored.sort(Comparator
                .comparing((IdentityMatchCandidateDto c) -> c.getMatchType() == IdentityMatchType.EXACT_MATCH)
                .reversed()
                .thenComparing(IdentityMatchCandidateDto::getScore, Comparator.reverseOrder()));

        IdentityMatchType overall = IdentityMatchType.NO_MATCH;
        if (!scored.isEmpty()) {
            overall = scored.stream().anyMatch(c -> c.getMatchType() == IdentityMatchType.EXACT_MATCH)
                    ? IdentityMatchType.EXACT_MATCH
                    : IdentityMatchType.PROBABLE_MATCH;
        }

        return IdentityResolutionResultDto.builder()
                .matchType(overall)
                .candidates(scored)
                .build();
    }

    private UUID requireTenantId() {
        String raw = TenantContext.getTenantId();
        if (!StringUtils.hasText(raw)) {
            throw new ValidationException("Tenant context is required");
        }
        return UUID.fromString(raw);
    }

    private static String trim(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }
}
