package com.aisales.integration.application.service;

import com.aisales.common.contracts.client.LeadServiceClient;
import com.aisales.common.contracts.lead.CreateLeadRequest;
import com.aisales.common.contracts.lead.LeadDto;
import com.aisales.common.core.dto.ApiResponse;
import com.aisales.common.core.util.TenantContext;
import com.aisales.common.exception.exception.BusinessException;
import com.aisales.common.exception.exception.NotFoundException;
import com.aisales.common.exception.exception.UnauthorizedException;
import com.aisales.common.exception.exception.ValidationException;
import com.aisales.common.exception.model.ErrorCode;
import com.aisales.integration.application.dto.StubLeadAdsPayload;
import com.aisales.integration.domain.entity.MetaPageBinding;
import com.aisales.integration.infrastructure.configuration.MetaLeadAdsProperties;
import com.aisales.integration.infrastructure.meta.MetaGraphLeadClient;
import com.aisales.integration.infrastructure.meta.MetaWebhookSignatureVerifier;
import com.aisales.integration.infrastructure.persistence.IntegrationWebhookEventRepository;
import com.aisales.integration.infrastructure.persistence.MetaPageBindingRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.StringUtils;

/**
 * Ingests Meta Lead Ads webhooks, creates a Lead, then runs instant voice qualify.
 * Graph fetch and Lead Feign calls run outside the webhook-claim transaction.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MetaLeadAdsWebhookService {

    private static final String PROVIDER = "META_LEAD_ADS";

    private final MetaLeadAdsProperties properties;
    private final MetaPageBindingRepository metaPageBindingRepository;
    private final IntegrationWebhookEventRepository webhookEventRepository;
    private final LeadServiceClient leadServiceClient;
    private final InstantVoiceQualifyService instantVoiceQualifyService;
    private final MetaGraphLeadClient metaGraphLeadClient;
    private final ObjectMapper objectMapper;
    private final PlatformTransactionManager transactionManager;

    public String verifySubscription(String mode, String verifyToken, String challenge) {
        if (!properties.isEnabled()) {
            throw new ValidationException("Meta Lead Ads webhook is disabled");
        }
        if (!"subscribe".equalsIgnoreCase(mode)) {
            throw new ValidationException("Unsupported hub.mode");
        }
        if (!StringUtils.hasText(properties.getVerifyToken())
                || !properties.getVerifyToken().equals(verifyToken)) {
            throw new UnauthorizedException("Invalid Meta verify token");
        }
        return challenge != null ? challenge : "";
    }

    public LeadDto handle(String rawBody, String signatureHeader) {
        if (!properties.isEnabled()) {
            throw new ValidationException("Meta Lead Ads webhook is disabled");
        }

        boolean signatureRequired = !"STUB".equalsIgnoreCase(properties.getMode());
        MetaWebhookSignatureVerifier.verify(
                rawBody, signatureHeader, properties.getAppSecret(), signatureRequired);

        ResolvedLeadEnvelope envelope = resolveEnvelope(rawBody);

        MetaPageBinding binding = metaPageBindingRepository
                .findByPageIdAndActiveTrue(envelope.pageId())
                .orElseThrow(() -> new NotFoundException(
                        "No active Meta page binding for pageId=" + envelope.pageId()));

        StubLeadAdsPayload payload = envelope.payload();
        if (payload == null || !hasContactFields(payload)) {
            if (!"LIVE".equalsIgnoreCase(properties.getMode())) {
                throw new ValidationException(
                        "STUB mode requires full lead fields (fullName, phone). "
                                + "Meta leadgen_id-only envelopes need mode=LIVE.");
            }
            // Graph HTTP outside DB TX.
            payload = metaGraphLeadClient.fetchLead(
                    envelope.leadgenId(), envelope.pageId(), binding.getPageAccessToken());
        }

        validatePayload(payload);

        String eventId = "meta_leadgen_" + payload.getLeadgenId();
        Boolean claimed = new TransactionTemplate(transactionManager).execute(status ->
                webhookEventRepository.insertIgnoreConflict(
                        eventId, PROVIDER, "leadgen", Instant.now())
                        > 0);
        if (claimed == null || !claimed) {
            log.debug("Duplicate Meta leadgen ignored id={}", payload.getLeadgenId());
            return null;
        }

        TenantContext.setTenantId(binding.getTenantId().toString());
        if (binding.getOrganizationId() != null) {
            TenantContext.setOrganizationId(binding.getOrganizationId().toString());
        }
        TenantContext.setUserId("meta-lead-ads");

        try {
            String sourceType = resolveSourceType(payload, binding);
            Map<String, Object> attributes = payload.getFields() != null
                    ? new HashMap<>(payload.getFields())
                    : new HashMap<>();

            CreateLeadRequest createRequest = CreateLeadRequest.builder()
                    .customerName(payload.getFullName().trim())
                    .phone(payload.getPhone().trim())
                    .email(StringUtils.hasText(payload.getEmail()) ? payload.getEmail().trim() : null)
                    .sourceType(sourceType)
                    .sourceId(payload.getLeadgenId())
                    .campaign(StringUtils.hasText(payload.getCampaign())
                            ? payload.getCampaign()
                            : binding.getCampaignDefault())
                    .attributes(attributes)
                    .build();

            ApiResponse<LeadDto> created = leadServiceClient.createLead(createRequest);
            if (created == null || created.getData() == null || created.getData().getId() == null) {
                throw new BusinessException(ErrorCode.INTERNAL_ERROR, "Lead create from Meta Lead Ads failed");
            }
            LeadDto lead = created.getData();
            log.info(
                    "Created lead {} from Meta Lead Ads page={} leadgen={}",
                    lead.getId(),
                    payload.getPageId(),
                    payload.getLeadgenId());

            if (binding.isVoiceQualifyEnabled()) {
                lead = instantVoiceQualifyService.qualify(lead.getId(), binding, attributes);
            }
            return lead;
        } finally {
            TenantContext.clear();
        }
    }

    private ResolvedLeadEnvelope resolveEnvelope(String rawBody) {
        try {
            JsonNode root = objectMapper.readTree(rawBody);
            if (root.has("leadgenId") || root.has("pageId")) {
                StubLeadAdsPayload stub = objectMapper.treeToValue(root, StubLeadAdsPayload.class);
                return new ResolvedLeadEnvelope(
                        stub.getPageId(), stub.getLeadgenId(), stub);
            }
            JsonNode value = root.path("entry").path(0).path("changes").path(0).path("value");
            if (!value.isMissingNode() && value.has("leadgen_id")) {
                String leadgenId = value.path("leadgen_id").asText(null);
                String pageId = value.path("page_id").asText(null);
                if (!StringUtils.hasText(pageId)) {
                    pageId = root.path("entry").path(0).path("id").asText(null);
                }
                if (!StringUtils.hasText(leadgenId) || !StringUtils.hasText(pageId)) {
                    throw new ValidationException("Meta envelope missing leadgen_id or page_id");
                }
                return new ResolvedLeadEnvelope(pageId, leadgenId, null);
            }
            throw new ValidationException("Unrecognized Meta Lead Ads payload");
        } catch (BusinessException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new ValidationException("Invalid Meta Lead Ads JSON");
        }
    }

    private static boolean hasContactFields(StubLeadAdsPayload payload) {
        return payload != null
                && StringUtils.hasText(payload.getFullName())
                && StringUtils.hasText(payload.getPhone());
    }

    private static void validatePayload(StubLeadAdsPayload payload) {
        if (payload == null) {
            throw new ValidationException("Payload is required");
        }
        if (!StringUtils.hasText(payload.getPageId())) {
            throw new ValidationException("pageId is required");
        }
        if (!StringUtils.hasText(payload.getLeadgenId())) {
            throw new ValidationException("leadgenId is required");
        }
        if (!StringUtils.hasText(payload.getFullName())) {
            throw new ValidationException("fullName is required");
        }
        if (!StringUtils.hasText(payload.getPhone())) {
            throw new ValidationException("phone is required");
        }
    }

    private static String resolveSourceType(StubLeadAdsPayload payload, MetaPageBinding binding) {
        if (StringUtils.hasText(payload.getPlatform())) {
            String platform = payload.getPlatform().trim().toUpperCase();
            if (platform.contains("INSTA") || "IG".equals(platform)) {
                return "INSTAGRAM_LEAD_ADS";
            }
        }
        return StringUtils.hasText(binding.getSourceType())
                ? binding.getSourceType()
                : "FACEBOOK_LEAD_ADS";
    }

    private record ResolvedLeadEnvelope(String pageId, String leadgenId, StubLeadAdsPayload payload) {
    }
}
