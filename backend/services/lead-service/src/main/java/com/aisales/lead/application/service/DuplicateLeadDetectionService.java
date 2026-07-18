package com.aisales.lead.application.service;

import com.aisales.lead.domain.entity.Lead;
import com.aisales.lead.domain.entity.LeadDuplicate;
import com.aisales.lead.infrastructure.persistence.LeadDuplicateRepository;
import com.aisales.lead.infrastructure.persistence.LeadRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class DuplicateLeadDetectionService {

    private final LeadRepository leadRepository;
    private final LeadDuplicateRepository leadDuplicateRepository;

    public void detectAndRecord(Lead lead) {
        String phone = StringUtils.hasText(lead.getPhone()) ? lead.getPhone() : null;
        String email = StringUtils.hasText(lead.getEmail()) ? lead.getEmail() : null;
        if (phone == null && email == null) {
            return;
        }
        List<Lead> matches = leadRepository.findPotentialDuplicates(
                lead.getTenantId(), lead.getId(), phone, email);
        Instant now = Instant.now();
        for (Lead match : matches) {
            leadDuplicateRepository.save(LeadDuplicate.builder()
                    .tenantId(lead.getTenantId())
                    .leadId(lead.getId())
                    .duplicateOfLeadId(match.getId())
                    .similarityScore(BigDecimal.ONE)
                    .detectedAt(now)
                    .resolved(false)
                    .build());
        }
    }
}
