package com.aisales.integration.application.service;

import com.aisales.common.contracts.client.LeadServiceClient;
import com.aisales.common.contracts.lead.LeadDto;
import com.aisales.common.contracts.lead.UpdateLeadRequest;
import com.aisales.common.core.dto.ApiResponse;
import com.aisales.common.core.util.TenantContext;
import com.aisales.integration.domain.entity.VoiceCall;
import com.aisales.integration.infrastructure.persistence.VoiceCallRepository;
import com.aisales.integration.infrastructure.voice.TwilioVoiceProvider;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * Applies Twilio call status callbacks using {@code voice_call} lookup (CallSid → tenant/lead).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TwilioVoiceStatusService {

    private final VoiceCallRepository voiceCallRepository;
    private final LeadServiceClient leadServiceClient;

    @Transactional
    public void onStatus(String callSid, String callStatus, String to, String from) {
        if (!StringUtils.hasText(callSid)) {
            return;
        }
        String normalized = callStatus != null ? callStatus.trim().toUpperCase() : "UNKNOWN";
        log.info("Twilio voice status sid={} status={} to={}", callSid, normalized, to);

        Optional<VoiceCall> found =
                voiceCallRepository.findByProviderAndProviderCallId(TwilioVoiceProvider.NAME, callSid);
        if (found.isEmpty()) {
            log.debug("No voice_call row for Twilio sid={}", callSid);
            return;
        }

        VoiceCall voiceCall = found.get();
        voiceCall.setStatus(normalized);
        voiceCall.setUpdatedAt(Instant.now());
        voiceCallRepository.save(voiceCall);

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
