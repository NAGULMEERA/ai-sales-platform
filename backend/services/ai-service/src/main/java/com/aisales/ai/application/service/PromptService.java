package com.aisales.ai.application.service;

import com.aisales.ai.application.mapper.AiMapper;
import com.aisales.ai.domain.entity.PromptTemplate;
import com.aisales.ai.domain.entity.PromptVersionEntity;
import com.aisales.ai.domain.prompt.PlatformPromptConstants;
import com.aisales.ai.infrastructure.persistence.PromptTemplateRepository;
import com.aisales.ai.infrastructure.persistence.PromptVersionRepository;
import com.aisales.common.contracts.ai.CreatePromptRequest;
import com.aisales.common.contracts.ai.CreatePromptVersionRequest;
import com.aisales.common.contracts.ai.PromptDto;
import com.aisales.common.contracts.ai.PromptStatus;
import com.aisales.common.contracts.ai.PromptVersionDto;
import com.aisales.common.core.dto.PageResponse;
import com.aisales.common.core.util.TenantContext;
import com.aisales.common.exception.exception.NotFoundException;
import com.aisales.common.exception.exception.ValidationException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class PromptService {

    private final PromptTemplateRepository promptTemplateRepository;
    private final PromptVersionRepository promptVersionRepository;
    private final AiMapper mapper;

    @Transactional
    public PromptDto create(CreatePromptRequest request) {
        UUID tenantId = requireTenantId();
        UUID actor = actorId();
        Instant now = Instant.now();
        String code = request.getCode().trim().toUpperCase();

        if (promptTemplateRepository.existsByTenantIdAndCodeAndDeletedAtIsNull(tenantId, code)) {
            throw new ValidationException("Prompt code already exists: " + code);
        }

        PromptTemplate template = PromptTemplate.builder()
                .tenantId(tenantId)
                .organizationId(parseUuidOrNull(TenantContext.getOrganizationId()))
                .code(code)
                .name(request.getName().trim())
                .purpose(request.getPurpose().trim())
                .industryCode(normalizeDimension(request.getIndustryCode()))
                .languageCode(normalizeLanguage(request.getLanguageCode()))
                .capability(normalizeDimension(request.getCapability()))
                .preferredModel(trimToNull(request.getPreferredModel()))
                .status(PromptStatus.ACTIVE)
                .activeVersion(1)
                .createdAt(now)
                .updatedAt(now)
                .createdBy(actor)
                .updatedBy(actor)
                .build();
        PromptTemplate saved = promptTemplateRepository.saveAndFlush(template);

        PromptVersionEntity version = PromptVersionEntity.builder()
                .tenantId(tenantId)
                .promptId(saved.getId())
                .versionNumber(1)
                .systemTemplate(trimToNull(request.getSystemTemplate()))
                .userTemplate(request.getUserTemplate().trim())
                .variables(normalizeVariables(request.getVariables()))
                .expectedOutputHint(trimToNull(request.getExpectedOutputHint()))
                .changelog(StringUtils.hasText(request.getChangelog())
                        ? request.getChangelog().trim()
                        : "Initial version")
                .status(PromptStatus.ACTIVE)
                .createdAt(now)
                .createdBy(actor)
                .build();
        PromptVersionEntity savedVersion = promptVersionRepository.saveAndFlush(version);
        return mapper.toDto(saved, savedVersion);
    }

    @Transactional(readOnly = true)
    public PromptDto get(UUID id) {
        PromptTemplate template = requireTemplate(id);
        return mapper.toDto(template, latestVersion(template));
    }

    @Transactional(readOnly = true)
    public PromptDto getByCode(String code) {
        PromptTemplate template = findByCodeWithPlatformFallback(requireTenantId(), code.trim().toUpperCase())
                .orElseThrow(() -> new NotFoundException("Prompt not found: " + code));
        return mapper.toDto(template, latestVersion(template));
    }

    @Transactional(readOnly = true)
    public PageResponse<PromptDto> list(int page, int size) {
        UUID tenantId = requireTenantId();
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 100);
        Page<PromptTemplate> result = promptTemplateRepository
                .findByTenantIdAndDeletedAtIsNullOrderByUpdatedAtDesc(
                        tenantId, PageRequest.of(safePage, safeSize));
        return PageResponse.<PromptDto>builder()
                .content(result.getContent().stream()
                        .map(t -> mapper.toDto(t, latestVersion(t)))
                        .toList())
                .page(result.getNumber())
                .size(result.getSize())
                .totalElements(result.getTotalElements())
                .totalPages(result.getTotalPages())
                .first(result.isFirst())
                .last(result.isLast())
                .build();
    }

    @Transactional
    public PromptVersionDto createVersion(UUID promptId, CreatePromptVersionRequest request) {
        UUID tenantId = requireTenantId();
        UUID actor = actorId();
        PromptTemplate template = requireTemplate(promptId);
        int next = promptVersionRepository.findMaxVersion(tenantId, promptId) + 1;

        PromptVersionEntity version = PromptVersionEntity.builder()
                .tenantId(tenantId)
                .promptId(promptId)
                .versionNumber(next)
                .systemTemplate(trimToNull(request.getSystemTemplate()))
                .userTemplate(request.getUserTemplate().trim())
                .variables(normalizeVariables(request.getVariables()))
                .expectedOutputHint(trimToNull(request.getExpectedOutputHint()))
                .changelog(trimToNull(request.getChangelog()))
                .status(Boolean.TRUE.equals(request.getActivate())
                        ? PromptStatus.ACTIVE
                        : PromptStatus.DRAFT)
                .createdAt(Instant.now())
                .createdBy(actor)
                .build();
        PromptVersionEntity saved = promptVersionRepository.saveAndFlush(version);

        if (Boolean.TRUE.equals(request.getActivate())) {
            archiveActiveVersions(tenantId, promptId, saved.getId());
            template.activateVersion(saved.getVersionNumber(), actor);
            promptTemplateRepository.save(template);
        }
        return mapper.toVersionDto(saved);
    }

    @Transactional(readOnly = true)
    public List<PromptVersionDto> listVersions(UUID promptId) {
        requireTemplate(promptId);
        return promptVersionRepository
                .findByTenantIdAndPromptIdOrderByVersionNumberDesc(requireTenantId(), promptId)
                .stream()
                .map(mapper::toVersionDto)
                .toList();
    }

    /**
     * Marks a DRAFT version as APPROVED (eligible for activation). Does not change active_version.
     */
    @Transactional
    public PromptVersionDto approveVersion(UUID promptId, int versionNumber) {
        PromptTemplate template = requireTemplate(promptId);
        PromptVersionEntity version = requireVersion(template, versionNumber);
        if (version.getStatus() == PromptStatus.ARCHIVED) {
            throw new ValidationException("Cannot approve an archived prompt version");
        }
        if (version.getStatus() == PromptStatus.ACTIVE) {
            return mapper.toVersionDto(version);
        }
        version.setStatus(PromptStatus.APPROVED);
        return mapper.toVersionDto(promptVersionRepository.save(version));
    }

    /**
     * Activates an APPROVED or DRAFT version and archives other ACTIVE versions.
     */
    @Transactional
    public PromptDto activateVersion(UUID promptId, int versionNumber) {
        UUID actor = actorId();
        PromptTemplate template = requireTemplate(promptId);
        PromptVersionEntity version = requireVersion(template, versionNumber);
        if (version.getStatus() == PromptStatus.ARCHIVED) {
            throw new ValidationException("Cannot activate an archived prompt version");
        }
        archiveActiveVersions(template.getTenantId(), promptId, version.getId());
        version.setStatus(PromptStatus.ACTIVE);
        promptVersionRepository.save(version);
        template.activateVersion(version.getVersionNumber(), actor);
        if (template.getStatus() == PromptStatus.DRAFT || template.getStatus() == PromptStatus.ARCHIVED) {
            template.setStatus(PromptStatus.ACTIVE);
        }
        promptTemplateRepository.save(template);
        return mapper.toDto(template, version);
    }

    /**
     * Rolls back to a prior version by reactivating it (including ARCHIVED versions).
     * Previous ACTIVE versions become ARCHIVED.
     */
    @Transactional
    public PromptDto rollbackToVersion(UUID promptId, int versionNumber) {
        UUID actor = actorId();
        PromptTemplate template = requireTemplate(promptId);
        PromptVersionEntity version = requireVersion(template, versionNumber);
        archiveActiveVersions(template.getTenantId(), promptId, version.getId());
        version.setStatus(PromptStatus.ACTIVE);
        promptVersionRepository.save(version);
        template.activateVersion(version.getVersionNumber(), actor);
        if (template.getStatus() == PromptStatus.DRAFT || template.getStatus() == PromptStatus.ARCHIVED) {
            template.setStatus(PromptStatus.ACTIVE);
        }
        promptTemplateRepository.save(template);
        return mapper.toDto(template, version);
    }

    private PromptVersionEntity requireVersion(PromptTemplate template, int versionNumber) {
        return promptVersionRepository
                .findByTenantIdAndPromptIdAndVersionNumber(
                        template.getTenantId(), template.getId(), versionNumber)
                .orElseThrow(() -> new NotFoundException(
                        "Prompt version not found: " + template.getCode() + " v" + versionNumber));
    }

    @Transactional(readOnly = true)
    public ResolvedPrompt resolveForExecution(String promptCode, UUID promptId, Integer versionNumber) {
        return resolveForExecution(promptCode, promptId, versionNumber, null, null, null);
    }

    @Transactional(readOnly = true)
    public ResolvedPrompt resolveForExecution(
            String promptCode,
            UUID promptId,
            Integer versionNumber,
            String industryCode,
            String languageCode,
            String capability) {
        PromptTemplate template;
        if (promptId != null) {
            template = requireTemplate(promptId);
        } else if (StringUtils.hasText(promptCode)) {
            template = findByCodeWithPlatformFallback(requireTenantId(), promptCode.trim().toUpperCase())
                    .orElseThrow(() -> new NotFoundException("Prompt not found: " + promptCode));
        } else {
            String industry = normalizeDimension(industryCode);
            String cap = normalizeDimension(capability);
            if (!StringUtils.hasText(industry) || !StringUtils.hasText(cap)) {
                throw new ValidationException(
                        "promptCode, promptId, or industryCode+capability is required");
            }
            template = findByDimensionsWithPlatformFallback(
                            requireTenantId(), industry, normalizeLanguage(languageCode), cap)
                    .orElseThrow(() -> new NotFoundException(
                            "Prompt not found for industry=" + industry + " capability=" + cap));
        }
        template.assertActiveRecord();
        if (template.getStatus() == PromptStatus.ARCHIVED) {
            throw new ValidationException("Prompt is archived: " + template.getCode());
        }

        int version = versionNumber != null
                ? versionNumber
                : (template.getActiveVersion() != null ? template.getActiveVersion() : 1);
        // Version rows are owned by the template's tenant (tenant override or platform seed).
        PromptVersionEntity promptVersion = promptVersionRepository
                .findByTenantIdAndPromptIdAndVersionNumber(template.getTenantId(), template.getId(), version)
                .orElseThrow(() -> new NotFoundException(
                        "Prompt version not found: " + template.getCode() + " v" + version));
        return new ResolvedPrompt(template, promptVersion);
    }

    private Optional<PromptTemplate> findByCodeWithPlatformFallback(UUID tenantId, String code) {
        Optional<PromptTemplate> tenantPrompt =
                promptTemplateRepository.findByTenantIdAndCodeAndDeletedAtIsNull(tenantId, code);
        if (tenantPrompt.isPresent()) {
            return tenantPrompt;
        }
        if (PlatformPromptConstants.PLATFORM_TENANT_ID.equals(tenantId)) {
            return Optional.empty();
        }
        return promptTemplateRepository.findByTenantIdAndCodeAndDeletedAtIsNull(
                PlatformPromptConstants.PLATFORM_TENANT_ID, code);
    }

    private Optional<PromptTemplate> findByDimensionsWithPlatformFallback(
            UUID tenantId, String industryCode, String languageCode, String capability) {
        Optional<PromptTemplate> tenantMatch =
                firstDimensionMatch(tenantId, industryCode, languageCode, capability);
        if (tenantMatch.isPresent()) {
            return tenantMatch;
        }
        if (PlatformPromptConstants.PLATFORM_TENANT_ID.equals(tenantId)) {
            return Optional.empty();
        }
        return firstDimensionMatch(
                PlatformPromptConstants.PLATFORM_TENANT_ID, industryCode, languageCode, capability);
    }

    private Optional<PromptTemplate> firstDimensionMatch(
            UUID tenantId, String industryCode, String languageCode, String capability) {
        if (StringUtils.hasText(languageCode)) {
            List<PromptTemplate> exact = promptTemplateRepository.findByDimensionsExact(
                    tenantId, industryCode, capability, languageCode, PromptStatus.ACTIVE);
            if (!exact.isEmpty()) {
                return Optional.of(exact.get(0));
            }
        }
        List<PromptTemplate> agnostic = promptTemplateRepository.findByDimensionsLanguageAgnostic(
                tenantId, industryCode, capability, PromptStatus.ACTIVE);
        if (!agnostic.isEmpty()) {
            return Optional.of(agnostic.get(0));
        }
        if (!StringUtils.hasText(languageCode)) {
            List<PromptTemplate> anyLanguage = promptTemplateRepository.findByDimensionsExact(
                    tenantId, industryCode, capability, null, PromptStatus.ACTIVE);
            // Also try common default when caller omitted language.
            if (anyLanguage.isEmpty()) {
                anyLanguage = promptTemplateRepository.findByDimensionsExact(
                        tenantId, industryCode, capability, "en", PromptStatus.ACTIVE);
            }
            if (!anyLanguage.isEmpty()) {
                return Optional.of(anyLanguage.get(0));
            }
        }
        return Optional.empty();
    }

    private void archiveActiveVersions(UUID tenantId, UUID promptId, UUID keepId) {
        List<PromptVersionEntity> toArchive = new ArrayList<>();
        for (PromptVersionEntity existing :
                promptVersionRepository.findByTenantIdAndPromptIdOrderByVersionNumberDesc(tenantId, promptId)) {
            if (!existing.getId().equals(keepId) && existing.getStatus() == PromptStatus.ACTIVE) {
                existing.setStatus(PromptStatus.ARCHIVED);
                toArchive.add(existing);
            }
        }
        if (!toArchive.isEmpty()) {
            promptVersionRepository.saveAll(toArchive);
        }
    }

    private PromptVersionEntity latestVersion(PromptTemplate template) {
        List<PromptVersionEntity> versions = promptVersionRepository
                .findByTenantIdAndPromptIdOrderByVersionNumberDesc(template.getTenantId(), template.getId());
        return versions.isEmpty() ? null : versions.get(0);
    }

    private PromptTemplate requireTemplate(UUID id) {
        return promptTemplateRepository.findByTenantIdAndIdAndDeletedAtIsNull(requireTenantId(), id)
                .orElseThrow(() -> new NotFoundException("Prompt not found: " + id));
    }

    private static List<String> normalizeVariables(List<String> variables) {
        if (variables == null || variables.isEmpty()) {
            return new ArrayList<>();
        }
        return variables.stream()
                .filter(StringUtils::hasText)
                .map(String::trim)
                .distinct()
                .toList();
    }

    private UUID requireTenantId() {
        String raw = TenantContext.getTenantId();
        if (!StringUtils.hasText(raw)) {
            throw new ValidationException("Tenant context is required");
        }
        return UUID.fromString(raw);
    }

    private UUID actorId() {
        return parseUuidOrNull(TenantContext.getUserId());
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

    private static String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    private static String normalizeDimension(String value) {
        String trimmed = trimToNull(value);
        return trimmed == null ? null : trimmed.toUpperCase();
    }

    private static String normalizeLanguage(String value) {
        String trimmed = trimToNull(value);
        return trimmed == null ? null : trimmed.toLowerCase();
    }

    public record ResolvedPrompt(PromptTemplate template, PromptVersionEntity version) {
    }
}
