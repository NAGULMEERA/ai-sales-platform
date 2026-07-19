package com.aisales.billing.api.controller;

import com.aisales.billing.application.service.InvoiceService;
import com.aisales.billing.application.service.PaymentService;
import com.aisales.common.contracts.billing.CreateAiUsageInvoiceRequest;
import com.aisales.common.contracts.billing.InvoiceDto;
import com.aisales.common.contracts.billing.InvoiceStatus;
import com.aisales.common.contracts.billing.PayInvoiceRequest;
import com.aisales.common.contracts.billing.PaymentDto;
import com.aisales.common.core.dto.ApiResponse;
import com.aisales.common.core.dto.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/invoices")
@RequiredArgsConstructor
@Tag(name = "Invoices", description = "Tenant invoices + payment (STUB | STRIPE)")
public class InvoiceController {

    private final InvoiceService invoiceService;
    private final PaymentService paymentService;

    @PostMapping("/from-ai-usage")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create invoice from AI usage ledger for a period")
    public ApiResponse<InvoiceDto> createFromAiUsage(@Valid @RequestBody CreateAiUsageInvoiceRequest request) {
        return ApiResponse.ok(invoiceService.createFromAiUsage(request));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get invoice with line items")
    public ApiResponse<InvoiceDto> get(@PathVariable UUID id) {
        return ApiResponse.ok(invoiceService.get(id));
    }

    @GetMapping
    @Operation(summary = "List invoices for current tenant")
    public ApiResponse<PageResponse<InvoiceDto>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) InvoiceStatus status) {
        return ApiResponse.ok(invoiceService.list(page, size, status));
    }

    @PostMapping("/{id}/issue")
    @Operation(summary = "Issue a DRAFT invoice")
    public ApiResponse<InvoiceDto> issue(@PathVariable UUID id) {
        return ApiResponse.ok(invoiceService.issue(id));
    }

    @PostMapping("/{id}/pay")
    @Operation(summary = "Pay an ISSUED invoice via configured provider (STUB | STRIPE)")
    public ApiResponse<PaymentDto> pay(
            @PathVariable UUID id, @RequestBody(required = false) PayInvoiceRequest request) {
        return ApiResponse.ok(paymentService.payInvoice(
                id, request != null ? request : PayInvoiceRequest.builder().build()));
    }
}
