package com.aisales.integration.api.controller;

import com.aisales.integration.application.service.TwilioVoiceStatusService;
import io.swagger.v3.oas.annotations.Hidden;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Public Twilio voice status callback. Updates lead attributes by CallSid when present.
 * Signature-verified via {@code X-Twilio-Signature} whenever Twilio voice is enabled.
 */
@RestController
@RequestMapping("/api/v1/integrations/webhooks/twilio/voice")
@RequiredArgsConstructor
@Hidden
public class TwilioVoiceWebhookController {

    private final TwilioVoiceStatusService twilioVoiceStatusService;

    @PostMapping(value = "/status", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public ResponseEntity<Void> status(
            @RequestParam MultiValueMap<String, String> form,
            @RequestHeader(value = "X-Twilio-Signature", required = false) String signature) {
        Map<String, String> flat = form.toSingleValueMap();
        twilioVoiceStatusService.onStatus(flat, signature);
        return ResponseEntity.ok().build();
    }
}
