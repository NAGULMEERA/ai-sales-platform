package com.aisales.ai.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

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
import com.aisales.common.core.util.TenantContext;
import com.aisales.common.exception.exception.ValidationException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PromptServiceTest {

    @Mock private PromptTemplateRepository promptTemplateRepository;
    @Mock private PromptVersionRepository promptVersionRepository;

    private PromptService promptService;
    private UUID tenantId;

    @BeforeEach
    void setUp() {
        tenantId = UUID.randomUUID();
        TenantContext.setTenantId(tenantId.toString());
        TenantContext.setUserId(UUID.randomUUID().toString());
        promptService = new PromptService(
                promptTemplateRepository, promptVersionRepository, new AiMapper());
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void shouldCreatePromptWithVersionOne() {
        when(promptTemplateRepository.existsByTenantIdAndCodeAndDeletedAtIsNull(tenantId, "LEAD_QUALIFY"))
                .thenReturn(false);
        when(promptTemplateRepository.saveAndFlush(any(PromptTemplate.class))).thenAnswer(inv -> {
            PromptTemplate t = inv.getArgument(0);
            t.setId(UUID.randomUUID());
            return t;
        });
        when(promptVersionRepository.saveAndFlush(any(PromptVersionEntity.class))).thenAnswer(inv -> {
            PromptVersionEntity v = inv.getArgument(0);
            v.setId(UUID.randomUUID());
            return v;
        });

        PromptDto dto = promptService.create(CreatePromptRequest.builder()
                .code("lead_qualify")
                .name("Lead Qualify")
                .purpose("LEAD_QUALIFICATION")
                .userTemplate("Qualify lead {{leadName}}")
                .variables(List.of("leadName"))
                .build());

        assertThat(dto.getCode()).isEqualTo("LEAD_QUALIFY");
        assertThat(dto.getStatus()).isEqualTo(PromptStatus.ACTIVE);
        assertThat(dto.getActiveVersion()).isEqualTo(1);
        assertThat(dto.getLatestVersion().getVersionNumber()).isEqualTo(1);
    }

    @Test
    void shouldRejectDuplicateCode() {
        when(promptTemplateRepository.existsByTenantIdAndCodeAndDeletedAtIsNull(tenantId, "DUP"))
                .thenReturn(true);

        assertThatThrownBy(() -> promptService.create(CreatePromptRequest.builder()
                        .code("dup")
                        .name("Dup")
                        .purpose("TEST")
                        .userTemplate("x")
                        .build()))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("already exists");
    }

    @Test
    void shouldCreateAndActivateNewVersion() {
        UUID promptId = UUID.randomUUID();
        PromptTemplate template = PromptTemplate.builder()
                .id(promptId)
                .tenantId(tenantId)
                .code("P1")
                .name("P1")
                .purpose("TEST")
                .status(PromptStatus.ACTIVE)
                .activeVersion(1)
                .createdAt(java.time.Instant.now())
                .updatedAt(java.time.Instant.now())
                .build();
        when(promptTemplateRepository.findByTenantIdAndIdAndDeletedAtIsNull(tenantId, promptId))
                .thenReturn(Optional.of(template));
        when(promptVersionRepository.findMaxVersion(tenantId, promptId)).thenReturn(1);
        when(promptVersionRepository.findByTenantIdAndPromptIdOrderByVersionNumberDesc(tenantId, promptId))
                .thenReturn(List.of(PromptVersionEntity.builder()
                        .id(UUID.randomUUID())
                        .tenantId(tenantId)
                        .promptId(promptId)
                        .versionNumber(1)
                        .userTemplate("v1")
                        .status(PromptStatus.ACTIVE)
                        .createdAt(java.time.Instant.now())
                        .build()));
        when(promptVersionRepository.saveAndFlush(any(PromptVersionEntity.class))).thenAnswer(inv -> {
            PromptVersionEntity v = inv.getArgument(0);
            v.setId(UUID.randomUUID());
            return v;
        });
        when(promptTemplateRepository.save(any(PromptTemplate.class))).thenAnswer(inv -> inv.getArgument(0));
        when(promptVersionRepository.save(any(PromptVersionEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        PromptVersionDto version = promptService.createVersion(promptId, CreatePromptVersionRequest.builder()
                .userTemplate("Qualify {{leadName}} v2")
                .variables(List.of("leadName"))
                .activate(true)
                .build());

        assertThat(version.getVersionNumber()).isEqualTo(2);
        assertThat(version.getStatus()).isEqualTo(PromptStatus.ACTIVE);
        assertThat(template.getActiveVersion()).isEqualTo(2);
    }

    @Test
    void shouldResolvePlatformSeedWhenTenantHasNoOverride() {
        UUID platformPromptId = UUID.randomUUID();
        PromptTemplate platformTemplate = PromptTemplate.builder()
                .id(platformPromptId)
                .tenantId(PlatformPromptConstants.PLATFORM_TENANT_ID)
                .code("LEAD_QUALIFY_REAL_ESTATE")
                .name("RE Qualify")
                .purpose("LEAD_QUALIFICATION")
                .status(PromptStatus.ACTIVE)
                .activeVersion(1)
                .createdAt(java.time.Instant.now())
                .updatedAt(java.time.Instant.now())
                .build();
        PromptVersionEntity platformVersion = PromptVersionEntity.builder()
                .id(UUID.randomUUID())
                .tenantId(PlatformPromptConstants.PLATFORM_TENANT_ID)
                .promptId(platformPromptId)
                .versionNumber(1)
                .userTemplate("Qualify RE lead {{budget}}")
                .variables(List.of("budget"))
                .status(PromptStatus.ACTIVE)
                .createdAt(java.time.Instant.now())
                .build();

        when(promptTemplateRepository.findByTenantIdAndCodeAndDeletedAtIsNull(
                        tenantId, "LEAD_QUALIFY_REAL_ESTATE"))
                .thenReturn(Optional.empty());
        when(promptTemplateRepository.findByTenantIdAndCodeAndDeletedAtIsNull(
                        PlatformPromptConstants.PLATFORM_TENANT_ID, "LEAD_QUALIFY_REAL_ESTATE"))
                .thenReturn(Optional.of(platformTemplate));
        when(promptVersionRepository.findByTenantIdAndPromptIdAndVersionNumber(
                        PlatformPromptConstants.PLATFORM_TENANT_ID, platformPromptId, 1))
                .thenReturn(Optional.of(platformVersion));

        PromptService.ResolvedPrompt resolved =
                promptService.resolveForExecution("LEAD_QUALIFY_REAL_ESTATE", null, null);

        assertThat(resolved.template().getTenantId()).isEqualTo(PlatformPromptConstants.PLATFORM_TENANT_ID);
        assertThat(resolved.version().getUserTemplate()).contains("{{budget}}");
    }

    @Test
    void shouldResolveByIndustryAndCapabilityWithPlatformFallback() {
        UUID platformPromptId = UUID.randomUUID();
        PromptTemplate platformTemplate = PromptTemplate.builder()
                .id(platformPromptId)
                .tenantId(PlatformPromptConstants.PLATFORM_TENANT_ID)
                .code("LEAD_QUALIFY_REAL_ESTATE")
                .name("RE Qualify")
                .purpose("LEAD_QUALIFICATION")
                .industryCode("REAL_ESTATE")
                .capability("LEAD_QUALIFICATION")
                .languageCode("en")
                .status(PromptStatus.ACTIVE)
                .activeVersion(1)
                .createdAt(java.time.Instant.now())
                .updatedAt(java.time.Instant.now())
                .build();
        PromptVersionEntity platformVersion = PromptVersionEntity.builder()
                .id(UUID.randomUUID())
                .tenantId(PlatformPromptConstants.PLATFORM_TENANT_ID)
                .promptId(platformPromptId)
                .versionNumber(1)
                .userTemplate("Qualify RE lead {{budget}}")
                .variables(List.of("budget"))
                .status(PromptStatus.ACTIVE)
                .createdAt(java.time.Instant.now())
                .build();

        when(promptTemplateRepository.findByDimensionsExact(
                        tenantId, "REAL_ESTATE", "LEAD_QUALIFICATION", "en", PromptStatus.ACTIVE))
                .thenReturn(List.of());
        when(promptTemplateRepository.findByDimensionsLanguageAgnostic(
                        tenantId, "REAL_ESTATE", "LEAD_QUALIFICATION", PromptStatus.ACTIVE))
                .thenReturn(List.of());
        when(promptTemplateRepository.findByDimensionsExact(
                        PlatformPromptConstants.PLATFORM_TENANT_ID,
                        "REAL_ESTATE",
                        "LEAD_QUALIFICATION",
                        "en",
                        PromptStatus.ACTIVE))
                .thenReturn(List.of(platformTemplate));
        when(promptVersionRepository.findByTenantIdAndPromptIdAndVersionNumber(
                        PlatformPromptConstants.PLATFORM_TENANT_ID, platformPromptId, 1))
                .thenReturn(Optional.of(platformVersion));

        PromptService.ResolvedPrompt resolved = promptService.resolveForExecution(
                null, null, null, "real_estate", "en", "lead_qualification");

        assertThat(resolved.template().getCode()).isEqualTo("LEAD_QUALIFY_REAL_ESTATE");
        assertThat(resolved.template().getIndustryCode()).isEqualTo("REAL_ESTATE");
    }

    @Test
    void shouldApproveDraftVersionWithoutActivating() {
        UUID promptId = UUID.randomUUID();
        PromptTemplate template = PromptTemplate.builder()
                .id(promptId)
                .tenantId(tenantId)
                .code("P_APPROVE")
                .name("Approve")
                .purpose("TEST")
                .status(PromptStatus.ACTIVE)
                .activeVersion(1)
                .createdAt(java.time.Instant.now())
                .updatedAt(java.time.Instant.now())
                .build();
        PromptVersionEntity draft = PromptVersionEntity.builder()
                .id(UUID.randomUUID())
                .tenantId(tenantId)
                .promptId(promptId)
                .versionNumber(2)
                .userTemplate("v2")
                .status(PromptStatus.DRAFT)
                .createdAt(java.time.Instant.now())
                .build();
        when(promptTemplateRepository.findByTenantIdAndIdAndDeletedAtIsNull(tenantId, promptId))
                .thenReturn(Optional.of(template));
        when(promptVersionRepository.findByTenantIdAndPromptIdAndVersionNumber(tenantId, promptId, 2))
                .thenReturn(Optional.of(draft));
        when(promptVersionRepository.save(any(PromptVersionEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        PromptVersionDto approved = promptService.approveVersion(promptId, 2);

        assertThat(approved.getStatus()).isEqualTo(PromptStatus.APPROVED);
        assertThat(template.getActiveVersion()).isEqualTo(1);
    }

    @Test
    void shouldRollbackByActivatingPriorVersion() {
        UUID promptId = UUID.randomUUID();
        UUID v1Id = UUID.randomUUID();
        UUID v2Id = UUID.randomUUID();
        PromptTemplate template = PromptTemplate.builder()
                .id(promptId)
                .tenantId(tenantId)
                .code("P_ROLLBACK")
                .name("Rollback")
                .purpose("TEST")
                .status(PromptStatus.ACTIVE)
                .activeVersion(2)
                .createdAt(java.time.Instant.now())
                .updatedAt(java.time.Instant.now())
                .build();
        PromptVersionEntity v1 = PromptVersionEntity.builder()
                .id(v1Id)
                .tenantId(tenantId)
                .promptId(promptId)
                .versionNumber(1)
                .userTemplate("v1")
                .status(PromptStatus.ARCHIVED)
                .createdAt(java.time.Instant.now())
                .build();
        PromptVersionEntity v2 = PromptVersionEntity.builder()
                .id(v2Id)
                .tenantId(tenantId)
                .promptId(promptId)
                .versionNumber(2)
                .userTemplate("v2")
                .status(PromptStatus.ACTIVE)
                .createdAt(java.time.Instant.now())
                .build();
        when(promptTemplateRepository.findByTenantIdAndIdAndDeletedAtIsNull(tenantId, promptId))
                .thenReturn(Optional.of(template));
        when(promptVersionRepository.findByTenantIdAndPromptIdAndVersionNumber(tenantId, promptId, 1))
                .thenReturn(Optional.of(v1));
        when(promptVersionRepository.findByTenantIdAndPromptIdOrderByVersionNumberDesc(tenantId, promptId))
                .thenReturn(List.of(v2, v1));
        when(promptVersionRepository.save(any(PromptVersionEntity.class))).thenAnswer(inv -> inv.getArgument(0));
        when(promptTemplateRepository.save(any(PromptTemplate.class))).thenAnswer(inv -> inv.getArgument(0));

        PromptDto rolledBack = promptService.rollbackToVersion(promptId, 1);

        assertThat(rolledBack.getActiveVersion()).isEqualTo(1);
        assertThat(v1.getStatus()).isEqualTo(PromptStatus.ACTIVE);
        assertThat(v2.getStatus()).isEqualTo(PromptStatus.ARCHIVED);
    }
}
