package com.aisales.ai.application.mapper;

import com.aisales.ai.domain.entity.KnowledgeBase;
import com.aisales.ai.domain.entity.KnowledgeDocument;
import com.aisales.ai.domain.entity.PromptTemplate;
import com.aisales.ai.domain.entity.PromptVersionEntity;
import com.aisales.common.contracts.ai.KnowledgeBaseDto;
import com.aisales.common.contracts.ai.KnowledgeDocumentDto;
import com.aisales.common.contracts.ai.PromptDto;
import com.aisales.common.contracts.ai.PromptVersionDto;
import java.util.HashMap;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class AiMapper {

    public PromptDto toDto(PromptTemplate template, PromptVersionEntity latest) {
        return PromptDto.builder()
                .id(template.getId())
                .tenantId(template.getTenantId())
                .organizationId(template.getOrganizationId())
                .code(template.getCode())
                .name(template.getName())
                .purpose(template.getPurpose())
                .status(template.getStatus())
                .activeVersion(template.getActiveVersion())
                .latestVersion(latest != null ? toVersionDto(latest) : null)
                .createdAt(template.getCreatedAt())
                .updatedAt(template.getUpdatedAt())
                .version(template.getVersion())
                .build();
    }

    public PromptVersionDto toVersionDto(PromptVersionEntity version) {
        return PromptVersionDto.builder()
                .id(version.getId())
                .promptId(version.getPromptId())
                .versionNumber(version.getVersionNumber())
                .systemTemplate(version.getSystemTemplate())
                .userTemplate(version.getUserTemplate())
                .variables(version.getVariables() != null ? List.copyOf(version.getVariables()) : List.of())
                .expectedOutputHint(version.getExpectedOutputHint())
                .changelog(version.getChangelog())
                .status(version.getStatus())
                .createdAt(version.getCreatedAt())
                .build();
    }

    public KnowledgeBaseDto toDto(KnowledgeBase kb) {
        return KnowledgeBaseDto.builder()
                .id(kb.getId())
                .tenantId(kb.getTenantId())
                .organizationId(kb.getOrganizationId())
                .code(kb.getCode())
                .name(kb.getName())
                .description(kb.getDescription())
                .status(kb.getStatus())
                .metadata(kb.getMetadata() != null ? new HashMap<>(kb.getMetadata()) : new HashMap<>())
                .createdAt(kb.getCreatedAt())
                .updatedAt(kb.getUpdatedAt())
                .version(kb.getVersion())
                .build();
    }

    public KnowledgeDocumentDto toDto(KnowledgeDocument doc) {
        return KnowledgeDocumentDto.builder()
                .id(doc.getId())
                .tenantId(doc.getTenantId())
                .knowledgeBaseId(doc.getKnowledgeBaseId())
                .name(doc.getName())
                .contentType(doc.getContentType())
                .sizeBytes(doc.getSizeBytes())
                .mediaId(doc.getMediaId())
                .objectKey(doc.getObjectKey())
                .status(doc.getStatus())
                .metadata(doc.getMetadata() != null ? new HashMap<>(doc.getMetadata()) : new HashMap<>())
                .createdAt(doc.getCreatedAt())
                .updatedAt(doc.getUpdatedAt())
                .build();
    }
}
