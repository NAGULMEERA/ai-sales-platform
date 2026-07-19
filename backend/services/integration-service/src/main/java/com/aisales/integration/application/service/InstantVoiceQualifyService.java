package com.aisales.integration.application.service;

import com.aisales.common.contracts.client.LeadServiceClient;
import com.aisales.common.contracts.lead.AiLeadQualificationResultDto;
import com.aisales.common.contracts.lead.LeadDto;
import com.aisales.common.contracts.lead.QualifyLeadWithAiRequest;
import com.aisales.common.contracts.lead.UpdateLeadRequest;
import com.aisales.common.core.dto.ApiResponse;
import com.aisales.common.exception.exception.BusinessException;
import com.aisales.common.exception.model.ErrorCode;
import com.aisales.integration.domain.entity.MetaPageBinding;
import com.aisales.integration.domain.entity.VoiceCall;
import com.aisales.integration.domain.voice.VoiceProvider;
import com.aisales.integration.domain.voice.VoiceProvider.VoiceCallRequest;
import com.aisales.integration.domain.voice.VoiceProvider.VoiceCallResult;
import com.aisales.integration.infrastructure.persistence.VoiceCallRepository;
import com.aisales.integration.infrastructure.voice.VoiceProviderRegistry;
import java.time.Instant;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.StringUtils;

/**
 * Instant voice qualify runtime owned by integration-service.
 * Places an outbound call via the configured provider (STUB | TWILIO),
 * merges captured attributes, then optionally runs AI Gateway qualification.
 * Provider HTTP and Lead Feign calls run outside DB transactions.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InstantVoiceQualifyService {

    private final LeadServiceClient leadServiceClient;
    private final VoiceProviderRegistry voiceProviderRegistry;
    private final VoiceCallRepository voiceCallRepository;
    private final PlatformTransactionManager transactionManager;

    public LeadDto qualify(UUID leadId, MetaPageBinding binding, Map<String, Object> formFields) {
        ApiResponse<LeadDto> currentResponse = leadServiceClient.getLead(leadId);
        if (currentResponse == null || currentResponse.getData() == null) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "Lead not found for voice qualify: " + leadId);
        }
        LeadDto current = currentResponse.getData();

        Map<String, Object> attributes = new HashMap<>();
        if (formFields != null) {
            attributes.putAll(formFields);
        }

        VoiceProvider provider = voiceProviderRegistry.resolveDefault();
        VoiceCallResult call = provider.placeOutboundCall(new VoiceCallRequest(
                leadId,
                current.getTenantId(),
                current.getPhone(),
                current.getCustomerName(),
                Map.copyOf(attributes)));

        attributes.put("voiceChannel", provider.name());
        attributes.put("voiceCallStatus", call.status());
        if (StringUtils.hasText(call.providerCallId())) {
            attributes.put("voiceCallId", call.providerCallId());
            persistVoiceCall(current, provider.name(), call);
        }
        if (StringUtils.hasText(call.failureMessage())) {
            attributes.put("voiceFailureMessage", call.failureMessage());
        }
        if (call.capturedAttributes() != null) {
            call.capturedAttributes().forEach(attributes::putIfAbsent);
        }

        ApiResponse<LeadDto> updated = leadServiceClient.updateLead(
                leadId, UpdateLeadRequest.builder().attributes(attributes).build());
        if (updated == null || updated.getData() == null) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "Lead update after voice qualify failed");
        }

        try {
            leadServiceClient.contactLead(leadId, "VOICE");
        } catch (Exception ex) {
            log.warn("contactLead(VOICE) failed for lead {}: {}", leadId, ex.getMessage());
        }

        boolean hasQualificationMaterial = attributes.containsKey("budget")
                || attributes.containsKey("location")
                || attributes.containsKey("timeline");
        if (StringUtils.hasText(binding.getPromptCode()) && hasQualificationMaterial) {
            List<String> keys = parseKeys(binding.getQualificationVariableKeys());
            try {
                ApiResponse<AiLeadQualificationResultDto> ai = leadServiceClient.qualifyWithAi(
                        leadId,
                        QualifyLeadWithAiRequest.builder()
                                .promptCode(binding.getPromptCode())
                                .variableKeys(keys)
                                .notes("Instant voice qualify after social Lead Ads via " + provider.name())
                                .build());
                if (ai != null && ai.getData() != null && ai.getData().getLead() != null) {
                    log.info(
                            "AI qualify after voice lead={} provider={} recommendation={} qualified={}",
                            leadId,
                            provider.name(),
                            ai.getData().getRecommendation(),
                            ai.getData().isQualified());
                    return ai.getData().getLead();
                }
            } catch (Exception ex) {
                log.warn("AI qualify after voice failed for lead {}: {}", leadId, ex.getMessage());
            }
        }

        return updated.getData();
    }

    private void persistVoiceCall(LeadDto current, String provider, VoiceCallResult call) {
        TransactionTemplate tx = new TransactionTemplate(transactionManager);
        tx.executeWithoutResult(status -> {
            Instant now = Instant.now();
            voiceCallRepository.save(VoiceCall.builder()
                    .id(UUID.randomUUID())
                    .tenantId(current.getTenantId())
                    .leadId(current.getId())
                    .provider(provider)
                    .providerCallId(call.providerCallId())
                    .toPhone(current.getPhone())
                    .status(call.status())
                    .createdAt(now)
                    .updatedAt(now)
                    .build());
        });
    }

    private static List<String> parseKeys(String raw) {
        if (!StringUtils.hasText(raw)) {
            return List.of("budget", "location", "timeline");
        }
        return Arrays.stream(raw.split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .toList();
    }
}
