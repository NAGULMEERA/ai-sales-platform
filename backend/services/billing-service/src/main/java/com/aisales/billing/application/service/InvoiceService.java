package com.aisales.billing.application.service;

import com.aisales.billing.domain.entity.Invoice;
import com.aisales.billing.domain.entity.InvoiceLineItem;
import com.aisales.billing.infrastructure.persistence.InvoiceRepository;
import com.aisales.common.contracts.ai.AiUsageBreakdownDto;
import com.aisales.common.contracts.ai.AiUsageSummaryDto;
import com.aisales.common.contracts.billing.CreateAiUsageInvoiceRequest;
import com.aisales.common.contracts.billing.InvoiceDto;
import com.aisales.common.contracts.billing.InvoiceLineItemDto;
import com.aisales.common.contracts.billing.InvoiceStatus;
import com.aisales.common.contracts.client.AiServiceClient;
import com.aisales.common.core.dto.ApiResponse;
import com.aisales.common.core.dto.PageResponse;
import com.aisales.common.core.util.TenantContext;
import com.aisales.common.exception.exception.BusinessException;
import com.aisales.common.exception.exception.NotFoundException;
import com.aisales.common.exception.exception.ValidationException;
import com.aisales.common.exception.model.ErrorCode;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * Billing owns invoices. AI usage is fetched via Feign; amounts are snapshotted.
 * Payment collection is handled by {@link PaymentService}.
 */
@Service
@RequiredArgsConstructor
public class InvoiceService {

    public static final String SOURCE_AI_USAGE = "AI_USAGE";

    private final InvoiceRepository invoiceRepository;
    private final AiServiceClient aiServiceClient;

    @Transactional
    public InvoiceDto createFromAiUsage(CreateAiUsageInvoiceRequest request) {
        UUID tenantId = requireTenantId();
        validatePeriod(request.getPeriodFrom(), request.getPeriodTo());

        if (invoiceRepository.existsByTenantIdAndPeriodStartAndPeriodEndAndSourceAndDeletedAtIsNull(
                tenantId, request.getPeriodFrom(), request.getPeriodTo(), SOURCE_AI_USAGE)) {
            throw new BusinessException(
                    ErrorCode.CONFLICT,
                    "AI usage invoice already exists for this tenant and period");
        }

        AiUsageSummaryDto summary = fetchUsageSummary(request.getPeriodFrom(), request.getPeriodTo());
        Instant now = Instant.now();
        String actor = actor();
        UUID invoiceId = UUID.randomUUID();

        Invoice invoice = Invoice.builder()
                .id(invoiceId)
                .tenantId(tenantId)
                .organizationId(parseUuidOrNull(TenantContext.getOrganizationId()))
                .periodStart(request.getPeriodFrom())
                .periodEnd(request.getPeriodTo())
                .status(request.isIssue() ? InvoiceStatus.ISSUED : InvoiceStatus.DRAFT)
                .currency("USD")
                .source(SOURCE_AI_USAGE)
                .subtotalUsd(nullToZero(summary.getEstimatedCostUsd()))
                .totalUsd(nullToZero(summary.getEstimatedCostUsd()))
                .issuedAt(request.isIssue() ? now : null)
                .createdAt(now)
                .createdBy(actor)
                .updatedAt(now)
                .updatedBy(actor)
                .version(0L)
                .lineItems(new ArrayList<>())
                .build();

        List<AiUsageBreakdownDto> breakdown =
                summary.getBreakdown() != null ? summary.getBreakdown() : List.of();
        if (breakdown.isEmpty()) {
            invoice.getLineItems().add(lineItem(
                    invoice,
                    tenantId,
                    "AI_USAGE_NONE",
                    "No AI usage in period",
                    0L,
                    BigDecimal.ZERO,
                    BigDecimal.ZERO,
                    now));
        } else {
            for (AiUsageBreakdownDto row : breakdown) {
                BigDecimal lineTotal = nullToZero(row.getEstimatedCostUsd());
                long quantity = Math.max(0L, row.getTotalTokens());
                BigDecimal unit = quantity > 0
                        ? lineTotal.divide(BigDecimal.valueOf(quantity), 8, RoundingMode.HALF_UP)
                        : lineTotal;
                String code = "AI_" + safe(row.getOperation());
                String description = "AI %s %s %s".formatted(
                        safe(row.getOperation()), safe(row.getProvider()), safe(row.getModel()));
                invoice.getLineItems()
                        .add(lineItem(invoice, tenantId, code, description, quantity, unit, lineTotal, now));
            }
        }

        return toDto(invoiceRepository.save(invoice));
    }

    @Transactional(readOnly = true)
    public InvoiceDto get(UUID id) {
        return toDto(requireOwned(id));
    }

    @Transactional(readOnly = true)
    public PageResponse<InvoiceDto> list(int page, int size, InvoiceStatus status) {
        UUID tenantId = requireTenantId();
        PageRequest pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100));
        Page<Invoice> result = status != null
                ? invoiceRepository.findByTenantIdAndStatusAndDeletedAtIsNull(tenantId, status, pageable)
                : invoiceRepository.findByTenantIdAndDeletedAtIsNull(tenantId, pageable);
        List<InvoiceDto> content = result.getContent().stream().map(this::toDtoWithoutLines).toList();
        return PageResponse.<InvoiceDto>builder()
                .content(content)
                .page(result.getNumber())
                .size(result.getSize())
                .totalElements(result.getTotalElements())
                .totalPages(result.getTotalPages())
                .first(result.isFirst())
                .last(result.isLast())
                .build();
    }

    @Transactional
    public InvoiceDto issue(UUID id) {
        Invoice invoice = requireOwned(id);
        if (invoice.getStatus() == InvoiceStatus.ISSUED) {
            return toDto(invoice);
        }
        if (invoice.getStatus() != InvoiceStatus.DRAFT) {
            throw new ValidationException("Only DRAFT invoices can be issued");
        }
        Instant now = Instant.now();
        invoice.setStatus(InvoiceStatus.ISSUED);
        invoice.setIssuedAt(now);
        invoice.setUpdatedAt(now);
        invoice.setUpdatedBy(actor());
        return toDto(invoiceRepository.save(invoice));
    }

    private AiUsageSummaryDto fetchUsageSummary(Instant from, Instant to) {
        ApiResponse<AiUsageSummaryDto> response = aiServiceClient.getTokenUsageSummary(from, to);
        if (response == null || response.getData() == null) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "AI usage summary response was empty");
        }
        return response.getData();
    }

    private Invoice requireOwned(UUID id) {
        UUID tenantId = requireTenantId();
        return invoiceRepository
                .findByTenantIdAndIdAndDeletedAtIsNull(tenantId, id)
                .orElseThrow(() -> new NotFoundException("Invoice not found: " + id));
    }

    private static InvoiceLineItem lineItem(
            Invoice invoice,
            UUID tenantId,
            String code,
            String description,
            long quantity,
            BigDecimal unit,
            BigDecimal total,
            Instant now) {
        return InvoiceLineItem.builder()
                .id(UUID.randomUUID())
                .tenantId(tenantId)
                .invoice(invoice)
                .lineCode(code)
                .description(description)
                .quantity(quantity)
                .unitAmountUsd(unit)
                .lineTotalUsd(total)
                .createdAt(now)
                .build();
    }

    private InvoiceDto toDto(Invoice invoice) {
        List<InvoiceLineItemDto> lines = invoice.getLineItems() == null
                ? List.of()
                : invoice.getLineItems().stream().map(this::toLineDto).toList();
        return InvoiceDto.builder()
                .id(invoice.getId())
                .tenantId(invoice.getTenantId())
                .periodStart(invoice.getPeriodStart())
                .periodEnd(invoice.getPeriodEnd())
                .status(invoice.getStatus())
                .currency(invoice.getCurrency())
                .source(invoice.getSource())
                .subtotalUsd(invoice.getSubtotalUsd())
                .totalUsd(invoice.getTotalUsd())
                .issuedAt(invoice.getIssuedAt())
                .paidAt(invoice.getPaidAt())
                .createdAt(invoice.getCreatedAt())
                .lineItems(lines)
                .build();
    }

    private InvoiceDto toDtoWithoutLines(Invoice invoice) {
        return InvoiceDto.builder()
                .id(invoice.getId())
                .tenantId(invoice.getTenantId())
                .periodStart(invoice.getPeriodStart())
                .periodEnd(invoice.getPeriodEnd())
                .status(invoice.getStatus())
                .currency(invoice.getCurrency())
                .source(invoice.getSource())
                .subtotalUsd(invoice.getSubtotalUsd())
                .totalUsd(invoice.getTotalUsd())
                .issuedAt(invoice.getIssuedAt())
                .paidAt(invoice.getPaidAt())
                .createdAt(invoice.getCreatedAt())
                .lineItems(List.of())
                .build();
    }

    private InvoiceLineItemDto toLineDto(InvoiceLineItem item) {
        return InvoiceLineItemDto.builder()
                .id(item.getId())
                .lineCode(item.getLineCode())
                .description(item.getDescription())
                .quantity(item.getQuantity())
                .unitAmountUsd(item.getUnitAmountUsd())
                .lineTotalUsd(item.getLineTotalUsd())
                .build();
    }

    private static void validatePeriod(Instant from, Instant to) {
        if (from == null || to == null) {
            throw new ValidationException("periodFrom and periodTo are required");
        }
        if (!to.isAfter(from)) {
            throw new ValidationException("periodTo must be after periodFrom");
        }
    }

    private UUID requireTenantId() {
        String raw = TenantContext.getTenantId();
        if (!StringUtils.hasText(raw)) {
            throw new ValidationException("Tenant context is required");
        }
        return UUID.fromString(raw);
    }

    private static String actor() {
        return StringUtils.hasText(TenantContext.getUserId()) ? TenantContext.getUserId() : "system";
    }

    private static BigDecimal nullToZero(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }

    private static String safe(String value) {
        return StringUtils.hasText(value) ? value : "unknown";
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
}
