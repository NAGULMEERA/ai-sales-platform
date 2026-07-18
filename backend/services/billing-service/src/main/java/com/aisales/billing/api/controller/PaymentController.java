package com.aisales.billing.api.controller;

import com.aisales.billing.application.service.PaymentService;
import com.aisales.common.contracts.billing.PaymentDto;
import com.aisales.common.core.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
@Tag(name = "Payments", description = "Invoice payment records")
public class PaymentController {

    private final PaymentService paymentService;

    @GetMapping("/{id}")
    @Operation(summary = "Get a payment by id")
    public ApiResponse<PaymentDto> get(@PathVariable UUID id) {
        return ApiResponse.ok(paymentService.get(id));
    }
}
