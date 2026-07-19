package com.aisales.lead.application.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.aisales.common.contracts.lead.LeadStatus;
import com.aisales.lead.domain.entity.Lead;
import com.aisales.lead.infrastructure.persistence.LeadDuplicateRepository;
import com.aisales.lead.infrastructure.persistence.LeadRepository;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DuplicateLeadDetectionServiceTest {

    @Mock private LeadRepository leadRepository;
    @Mock private LeadDuplicateRepository duplicateRepository;

    private DuplicateLeadDetectionService service;
    private UUID tenantId;
    private UUID leadId;

    @BeforeEach
    void setUp() {
        service = new DuplicateLeadDetectionService(leadRepository, duplicateRepository);
        tenantId = UUID.randomUUID();
        leadId = UUID.randomUUID();
    }

    @Test
    void shouldMatchByPhoneEmailAndSourceId() {
        Lead lead = Lead.builder()
                .id(leadId)
                .tenantId(tenantId)
                .customerName("A")
                .phone("+911111111111")
                .email("a@example.com")
                .sourceType("META")
                .sourceId("ext-42")
                .status(LeadStatus.NEW)
                .attributes(new HashMap<>())
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
        Lead match = Lead.builder().id(UUID.randomUUID()).tenantId(tenantId).build();
        when(leadRepository.findPotentialDuplicates(
                        eq(tenantId),
                        eq(leadId),
                        eq("+911111111111"),
                        eq("a@example.com"),
                        isNull(),
                        eq("META"),
                        eq("ext-42")))
                .thenReturn(List.of(match));

        service.detectAndRecord(lead);

        verify(duplicateRepository).save(any());
    }

    @Test
    void shouldUseWhatsappAttributeAsIdentity() {
        Lead lead = Lead.builder()
                .id(leadId)
                .tenantId(tenantId)
                .customerName("B")
                .phone("+922222222222")
                .sourceType("WEB")
                .status(LeadStatus.NEW)
                .attributes(new HashMap<>(Map.of("whatsapp", "+933333333333")))
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
        when(leadRepository.findPotentialDuplicates(
                        eq(tenantId),
                        eq(leadId),
                        eq("+922222222222"),
                        isNull(),
                        eq("+933333333333"),
                        eq("WEB"),
                        isNull()))
                .thenReturn(List.of());

        service.detectAndRecord(lead);

        verify(leadRepository).findPotentialDuplicates(
                eq(tenantId),
                eq(leadId),
                eq("+922222222222"),
                isNull(),
                eq("+933333333333"),
                eq("WEB"),
                isNull());
    }
}
