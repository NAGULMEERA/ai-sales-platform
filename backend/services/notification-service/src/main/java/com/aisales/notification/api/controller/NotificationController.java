package com.aisales.notification.api.controller;

import com.aisales.common.contracts.notification.SendTransactionalEmailRequest;
import com.aisales.common.core.dto.ApiResponse;
import com.aisales.notification.application.service.EmailDeliveryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
@Tag(name = "Notifications", description = "Transactional email and notification delivery")
public class NotificationController {

    private final EmailDeliveryService emailDeliveryService;

    @PostMapping("/email")
    @Operation(summary = "Send a transactional email from a template")
    public ApiResponse<Void> sendTransactionalEmail(@Valid @RequestBody SendTransactionalEmailRequest request) {
        emailDeliveryService.sendTransactionalEmail(request);
        return ApiResponse.ok("Email queued for delivery", null);
    }
}
