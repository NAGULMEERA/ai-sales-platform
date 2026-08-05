package com.aisales.integration.application.service;

import com.aisales.common.contracts.client.LeadServiceClient;
import com.aisales.common.contracts.lead.LeadDto;
import com.aisales.common.contracts.lead.UpdateLeadRequest;
import com.aisales.common.core.dto.ApiResponse;
import com.aisales.common.core.util.TenantContext;
import com.aisales.integration.domain.entity.VoiceCall;
import com.aisales.integration.infrastructure.configuration.VoiceProperties;
import com.aisales.integration.infrastructure.persistence.VoiceCallRepository;
import com.aisales.integration.infrastructure.voice.TwilioVoiceProvider;
import com.aisales.integration.infrastructure.voice.TwilioWebhookSignatureVerifier;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.StringUtils;

/**
 * Applies Twilio call status callbacks using {@code voice_call} lookup (CallSid → tenant/lead).
 * Persist status in a short TX; Lead Feign updates run outside the transaction.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TwilioVoiceStatusService {

    private final VoiceCallRepository voiceCallRepository;
    private final LeadServiceClient leadServiceClient;
    private final PlatformTransactionManager transactionManager;
    private final VoiceProperties voiceProperties;

    public void onStatus(Map<String, String> formParams, String signatureHeader) {
        VoiceProperties.Twilio twilio = voiceProperties.getTwilio();
        TwilioWebhookSignatureVerifier.verify(
                twilio.getStatusCallbackUrl(), formParams, signatureHeader,
                twilio.getAuthToken(), twilio.isEnabled());

        String callSid = formParams.get("CallSid");
        String callStatus = formParams.get("CallStatus");
        String to = formParams.get("To");
        onStatus(callSid, callStatus, to, formParams.get("From"));
    }

    private void onStatus(String callSid, String callStatus, String to, String from) {
        if (!StringUtils.hasText(callSid)) {
            return;
        }
        String normalized = callStatus != null ? callStatus.trim().toUpperCase() : "UNKNOWN";
        log.info("Twilio voice status sid={} status={} to={}", callSid, normalized, to);

        VoiceCall voiceCall = new TransactionTemplate(transactionManager).execute(status -> {
            Optional<VoiceCall> found =
                    voiceCallRepository.findByProviderAndProviderCallId(TwilioVoiceProvider.NAME, callSid);
            if (found.isEmpty()) {
                log.debug("No voice_call row for Twilio sid={}", callSid);
                return null;
            }
            VoiceCall call = found.get();
            call.setStatus(normalized);
            call.setUpdatedAt(Instant.now());
            return voiceCallRepository.save(call);
        });

        if (voiceCall == null) {
            return;
        }

        TenantContext.setTenantId(voiceCall.getTenantId().toString());
        TenantContext.setUserId("twilio-voice");
        try {
            ApiResponse<LeadDto> leadResponse = leadServiceClient.getLead(voiceCall.getLeadId());
            if (leadResponse == null || leadResponse.getData() == null) {
                return;
            }
            LeadDto lead = leadResponse.getData();
            Map<String, Object> attrs = new HashMap<>(
                    lead.getAttributes() != null ? lead.getAttributes() : Map.of());
            attrs.put("voiceCallStatus", normalized);
            attrs.put("voiceCallId", callSid);
            leadServiceClient.updateLead(
                    lead.getId(), UpdateLeadRequest.builder().attributes(attrs).build());
        } catch (Exception ex) {
            log.warn("Twilio status lead update failed sid={}: {}", callSid, ex.getMessage());
        } finally {
            TenantContext.clear();
        }
    }
}
