package com.aisales.customer.application.service;

import com.aisales.customer.config.CustomerMatchingProperties;
import com.aisales.customer.domain.entity.Customer;
import com.aisales.customer.domain.entity.CustomerDuplicate;
import com.aisales.customer.infrastructure.persistence.CustomerDuplicateRepository;
import com.aisales.customer.infrastructure.persistence.CustomerRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class CustomerDuplicateDetectionService {

    private final CustomerRepository customerRepository;
    private final CustomerDuplicateRepository duplicateRepository;
    private final CustomerMatchingProperties matchingProperties;

    public void detectAndRecord(Customer customer) {
        String phone = matchingProperties.isMatchPhone() ? trim(customer.getPhone()) : null;
        String email = matchingProperties.isMatchEmail() ? trim(customer.getEmail()) : null;
        String whatsapp = matchingProperties.isMatchWhatsapp() ? trim(customer.getWhatsapp()) : null;
        String externalCrmId =
                matchingProperties.isMatchExternalCrmId() ? trim(customer.getExternalCrmId()) : null;
        String governmentId =
                matchingProperties.isMatchGovernmentId() ? trim(customer.getGovernmentId()) : null;

        if (phone == null && email == null && whatsapp == null
                && externalCrmId == null && governmentId == null) {
            return;
        }

        List<Customer> matches = customerRepository.findPotentialDuplicates(
                customer.getTenantId(),
                customer.getId(),
                phone,
                email,
                whatsapp,
                externalCrmId,
                governmentId);

        Instant now = Instant.now();
        for (Customer match : matches) {
            if (duplicateRepository.existsByTenantIdAndCustomerIdAndDuplicateOfCustomerIdAndResolvedFalse(
                    customer.getTenantId(), customer.getId(), match.getId())) {
                continue;
            }
            List<String> reasons = new ArrayList<>();
            int exact = 0;
            if (phone != null && phone.equals(match.getPhone())) {
                reasons.add("PHONE");
                exact++;
            }
            if (email != null && match.getEmail() != null && email.equalsIgnoreCase(match.getEmail())) {
                reasons.add("EMAIL");
                exact++;
            }
            if (whatsapp != null && whatsapp.equals(match.getWhatsapp())) {
                reasons.add("WHATSAPP");
                exact++;
            }
            if (externalCrmId != null && externalCrmId.equals(match.getExternalCrmId())) {
                reasons.add("EXTERNAL_CRM_ID");
                exact++;
            }
            if (governmentId != null && governmentId.equals(match.getGovernmentId())) {
                reasons.add("GOVERNMENT_ID");
                exact++;
            }
            if (reasons.isEmpty()) {
                continue;
            }

            String strength = exact >= 1 ? "EXACT" : "PROBABLE";
            if (exact == 0 && reasons.size() < matchingProperties.getWeakMatchThreshold()) {
                continue;
            }

            BigDecimal score = BigDecimal.valueOf(Math.min(1.0, 0.5 + (0.15 * exact)))
                    .setScale(4, RoundingMode.HALF_UP);

            duplicateRepository.save(CustomerDuplicate.builder()
                    .tenantId(customer.getTenantId())
                    .customerId(customer.getId())
                    .duplicateOfCustomerId(match.getId())
                    .matchReasons(String.join(",", reasons))
                    .similarityScore(score)
                    .matchStrength(strength)
                    .detectedAt(now)
                    .resolved(false)
                    .build());
        }
    }

    private static String trim(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }
}
