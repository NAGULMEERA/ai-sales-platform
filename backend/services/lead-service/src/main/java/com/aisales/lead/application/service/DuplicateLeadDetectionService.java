package com.aisales.lead.application.service;

import com.aisales.lead.domain.entity.Lead;
import com.aisales.lead.domain.entity.LeadDuplicate;
import com.aisales.lead.infrastructure.persistence.LeadDuplicateRepository;
import com.aisales.lead.infrastructure.persistence.LeadRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * Detects potential duplicates within a tenant using phone, email, WhatsApp, and external source IDs.
 */
@Service
@RequiredArgsConstructor
public class DuplicateLeadDetectionService {

    /** Cap candidate fan-out on shared phones/source IDs so create stays bounded. */
    private static final int MAX_DUPLICATE_CANDIDATES = 50;

    private final LeadRepository leadRepository;
    private final LeadDuplicateRepository leadDuplicateRepository;

    // Guards the read-then-save-N-duplicates sequence with its own transaction boundary so this
    // remains atomic even if called from a context other than LeadService.createLead's existing
    // transaction (no cross-service I/O here, so a local DB transaction is safe to hold).
    @Transactional
    public void detectAndRecord(Lead lead) {
        String phone = StringUtils.hasText(lead.getPhone()) ? lead.getPhone().trim() : null;
        String email = StringUtils.hasText(lead.getEmail()) ? lead.getEmail().trim() : null;
        String whatsapp = resolveWhatsapp(lead, phone);
        String sourceType = StringUtils.hasText(lead.getSourceType()) ? lead.getSourceType().trim() : null;
        String sourceId = StringUtils.hasText(lead.getSourceId()) ? lead.getSourceId().trim() : null;

        if (phone == null && email == null && whatsapp == null && sourceId == null) {
            return;
        }

        List<Lead> matches = leadRepository.findPotentialDuplicates(
                lead.getTenantId(),
                lead.getId(),
                phone,
                email,
                whatsapp,
                sourceType,
                sourceId,
                PageRequest.of(0, MAX_DUPLICATE_CANDIDATES));

        Instant now = Instant.now();
        List<LeadDuplicate> toSave = new ArrayList<>(matches.size());
        for (Lead match : matches) {
            toSave.add(LeadDuplicate.builder()
                    .tenantId(lead.getTenantId())
                    .leadId(lead.getId())
                    .duplicateOfLeadId(match.getId())
                    .similarityScore(BigDecimal.ONE)
                    .detectedAt(now)
                    .resolved(false)
                    .build());
        }
        if (!toSave.isEmpty()) {
            leadDuplicateRepository.saveAll(toSave);
        }
    }

    /**
     * WhatsApp identity is the E.164 phone for WhatsApp-sourced leads, or an explicit
     * {@code whatsapp} attribute when present.
     */
    private static String resolveWhatsapp(Lead lead, String phone) {
        Map<String, Object> attrs = lead.getAttributes();
        if (attrs != null) {
            Object attr = attrs.get("whatsapp");
            if (attr == null) {
                attr = attrs.get("whatsappNumber");
            }
            if (attr != null && StringUtils.hasText(Objects.toString(attr, null))) {
                return Objects.toString(attr).trim();
            }
        }
        if (phone != null && isWhatsAppSource(lead.getSourceType())) {
            return phone;
        }
        return null;
    }

    private static boolean isWhatsAppSource(String sourceType) {
        if (!StringUtils.hasText(sourceType)) {
            return false;
        }
        String normalized = sourceType.trim().toUpperCase(Locale.ROOT);
        return "WHATSAPP".equals(normalized) || "WA".equals(normalized);
    }
}
