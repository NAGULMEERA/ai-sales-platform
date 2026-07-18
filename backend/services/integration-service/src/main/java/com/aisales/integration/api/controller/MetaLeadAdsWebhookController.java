package com.aisales.integration.api.controller;

import com.aisales.common.contracts.lead.LeadDto;
import com.aisales.common.core.dto.ApiResponse;
import com.aisales.integration.application.service.MetaLeadAdsWebhookService;
import io.swagger.v3.oas.annotations.Hidden;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Public Meta Lead Ads webhook. GET verifies subscription; POST creates Lead and
 * runs STUB instant voice qualify.
 */
@RestController
@RequestMapping("/api/v1/integrations/webhooks/meta/leadgen")
@RequiredArgsConstructor
@Hidden
public class MetaLeadAdsWebhookController {

    private final MetaLeadAdsWebhookService metaLeadAdsWebhookService;

    @GetMapping(produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> verify(
            @RequestParam(name = "hub.mode", required = false) String mode,
            @RequestParam(name = "hub.verify_token", required = false) String verifyToken,
            @RequestParam(name = "hub.challenge", required = false) String challenge) {
        return ResponseEntity.ok(metaLeadAdsWebhookService.verifySubscription(mode, verifyToken, challenge));
    }

    @PostMapping
    public ApiResponse<LeadDto> ingest(
            @RequestBody String payload,
            @RequestHeader(value = "X-Hub-Signature-256", required = false) String signature) {
        LeadDto lead = metaLeadAdsWebhookService.handle(payload, signature);
        if (lead == null) {
            return ApiResponse.ok(null);
        }
        return ApiResponse.ok(lead);
    }
}
