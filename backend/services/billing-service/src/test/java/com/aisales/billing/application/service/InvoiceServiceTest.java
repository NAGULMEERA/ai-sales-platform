package com.aisales.billing.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.aisales.billing.domain.entity.Invoice;
import com.aisales.billing.infrastructure.persistence.InvoiceRepository;
import com.aisales.common.contracts.ai.AiUsageBreakdownDto;
import com.aisales.common.contracts.ai.AiUsageSummaryDto;
import com.aisales.common.contracts.billing.CreateAiUsageInvoiceRequest;
import com.aisales.common.contracts.billing.InvoiceDto;
import com.aisales.common.contracts.billing.InvoiceStatus;
import com.aisales.common.contracts.client.AiServiceClient;
import com.aisales.common.core.dto.ApiResponse;
import com.aisales.common.core.util.TenantContext;
import com.aisales.common.exception.exception.BusinessException;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class InvoiceServiceTest {

    @Mock private InvoiceRepository invoiceRepository;
    @Mock private AiServiceClient aiServiceClient;

    private InvoiceService service;
    private UUID tenantId;

    @BeforeEach
    void setUp() {
        tenantId = UUID.randomUUID();
        TenantContext.setTenantId(tenantId.toString());
        service = new InvoiceService(invoiceRepository, aiServiceClient);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void shouldCreateDraftInvoiceFromAiUsage() {
        Instant from = Instant.parse("2026-07-01T00:00:00Z");
        Instant to = Instant.parse("2026-08-01T00:00:00Z");
        when(invoiceRepository.existsByTenantIdAndPeriodStartAndPeriodEndAndSourceAndDeletedAtIsNull(
                        tenantId, from, to, InvoiceService.SOURCE_AI_USAGE))
                .thenReturn(false);
        when(aiServiceClient.getTokenUsageSummary(from, to))
                .thenReturn(ApiResponse.ok(AiUsageSummaryDto.builder()
                        .tenantId(tenantId)
                        .periodFrom(from)
                        .periodTo(to)
                        .totalTokens(150)
                        .estimatedCostUsd(new BigDecimal("0.012"))
                        .requestCount(2)
                        .breakdown(List.of(AiUsageBreakdownDto.builder()
                                .operation("EXECUTE")
                                .provider("gemini")
                                .model("gemini-2.0-flash")
                                .totalTokens(150)
                                .estimatedCostUsd(new BigDecimal("0.012"))
                                .requestCount(2)
                                .build()))
                        .build()));
        when(invoiceRepository.save(any(Invoice.class))).thenAnswer(inv -> inv.getArgument(0));

        InvoiceDto dto = service.createFromAiUsage(CreateAiUsageInvoiceRequest.builder()
                .periodFrom(from)
                .periodTo(to)
                .issue(false)
                .build());

        assertThat(dto.getStatus()).isEqualTo(InvoiceStatus.DRAFT);
        assertThat(dto.getSource()).isEqualTo(InvoiceService.SOURCE_AI_USAGE);
        assertThat(dto.getTotalUsd()).isEqualByComparingTo("0.012");
        assertThat(dto.getLineItems()).hasSize(1);
        ArgumentCaptor<Invoice> captor = ArgumentCaptor.forClass(Invoice.class);
        verify(invoiceRepository).save(captor.capture());
        assertThat(captor.getValue().getTenantId()).isEqualTo(tenantId);
    }

    @Test
    void shouldRejectDuplicatePeriod() {
        Instant from = Instant.parse("2026-07-01T00:00:00Z");
        Instant to = Instant.parse("2026-08-01T00:00:00Z");
        when(invoiceRepository.existsByTenantIdAndPeriodStartAndPeriodEndAndSourceAndDeletedAtIsNull(
                        eq(tenantId), eq(from), eq(to), eq(InvoiceService.SOURCE_AI_USAGE)))
                .thenReturn(true);

        assertThatThrownBy(() -> service.createFromAiUsage(CreateAiUsageInvoiceRequest.builder()
                        .periodFrom(from)
                        .periodTo(to)
                        .build()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("already exists");
    }
}
