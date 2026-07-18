package com.aisales.lead.application.mapper;

import com.aisales.common.contracts.lead.AssignmentPoolMemberDto;
import com.aisales.common.contracts.lead.LeadActivityDto;
import com.aisales.common.contracts.lead.LeadAttachmentDto;
import com.aisales.common.contracts.lead.LeadAttributionDto;
import com.aisales.common.contracts.lead.LeadCustomFieldDto;
import com.aisales.common.contracts.lead.LeadDto;
import com.aisales.common.contracts.lead.LeadDuplicateDto;
import com.aisales.common.contracts.lead.LeadFollowupDto;
import com.aisales.common.contracts.lead.LeadNoteDto;
import com.aisales.common.contracts.lead.LeadQualityScoreDto;
import com.aisales.common.contracts.lead.LeadStatusHistoryDto;
import com.aisales.lead.domain.entity.Lead;
import com.aisales.lead.domain.entity.LeadActivity;
import com.aisales.lead.domain.entity.LeadAssignmentPoolMember;
import com.aisales.lead.domain.entity.LeadAttachment;
import com.aisales.lead.domain.entity.LeadAttribution;
import com.aisales.lead.domain.entity.LeadCustomFieldDefinition;
import com.aisales.lead.domain.entity.LeadDuplicate;
import com.aisales.lead.domain.entity.LeadFollowup;
import com.aisales.lead.domain.entity.LeadNote;
import com.aisales.lead.domain.entity.LeadQualityScore;
import com.aisales.lead.domain.entity.LeadStatusHistory;
import org.springframework.stereotype.Component;

@Component
public class LeadMapper {

    public LeadDto toDto(Lead lead) {
        return LeadDto.builder()
                .id(lead.getId())
                .tenantId(lead.getTenantId())
                .organizationId(lead.getOrganizationId())
                .pipelineId(lead.getPipelineId())
                .customerId(lead.getCustomerId())
                .externalId(lead.getExternalId())
                .customerName(lead.getCustomerName())
                .phone(lead.getPhone())
                .email(lead.getEmail())
                .sourceType(lead.getSourceType())
                .sourceId(lead.getSourceId())
                .campaign(lead.getCampaign())
                .utmSource(lead.getUtmSource())
                .utmCampaign(lead.getUtmCampaign())
                .status(lead.getStatus())
                .validated(lead.isValidated())
                .qualified(lead.isQualified())
                .score(lead.getScore())
                .confidenceScore(lead.getConfidenceScore())
                .assignedTo(lead.getAssignedTo())
                .createdAt(lead.getCreatedAt())
                .updatedAt(lead.getUpdatedAt())
                .version(lead.getVersion())
                .build();
    }

    public LeadNoteDto toNoteDto(LeadNote note) {
        return LeadNoteDto.builder()
                .id(note.getId())
                .leadId(note.getLeadId())
                .note(note.getNote())
                .noteType(note.getNoteType())
                .createdBy(note.getCreatedBy())
                .createdAt(note.getCreatedAt())
                .build();
    }

    public LeadActivityDto toActivityDto(LeadActivity activity) {
        return LeadActivityDto.builder()
                .id(activity.getId())
                .leadId(activity.getLeadId())
                .activityType(activity.getActivityType())
                .description(activity.getDescription())
                .createdBy(activity.getCreatedBy())
                .createdAt(activity.getCreatedAt())
                .build();
    }

    public LeadFollowupDto toFollowupDto(LeadFollowup followup) {
        return LeadFollowupDto.builder()
                .id(followup.getId())
                .leadId(followup.getLeadId())
                .scheduledAt(followup.getScheduledAt())
                .completedAt(followup.getCompletedAt())
                .followupType(followup.getFollowupType())
                .note(followup.getNote())
                .assignedTo(followup.getAssignedTo())
                .createdBy(followup.getCreatedBy())
                .createdAt(followup.getCreatedAt())
                .build();
    }

    public LeadStatusHistoryDto toHistoryDto(LeadStatusHistory history) {
        return LeadStatusHistoryDto.builder()
                .id(history.getId())
                .leadId(history.getLeadId())
                .oldStatus(history.getOldStatus())
                .newStatus(history.getNewStatus())
                .reason(history.getReason())
                .changedBy(history.getChangedBy())
                .createdAt(history.getCreatedAt())
                .build();
    }

    public LeadDuplicateDto toDuplicateDto(LeadDuplicate duplicate) {
        return LeadDuplicateDto.builder()
                .id(duplicate.getId())
                .leadId(duplicate.getLeadId())
                .duplicateOfLeadId(duplicate.getDuplicateOfLeadId())
                .similarityScore(duplicate.getSimilarityScore())
                .resolved(duplicate.isResolved())
                .mergedIntoLeadId(duplicate.getMergedIntoLeadId())
                .detectedAt(duplicate.getDetectedAt())
                .build();
    }

    public LeadAttachmentDto toAttachmentDto(LeadAttachment attachment) {
        return LeadAttachmentDto.builder()
                .id(attachment.getId())
                .leadId(attachment.getLeadId())
                .fileName(attachment.getFileName())
                .fileUrl(attachment.getFileUrl())
                .fileType(attachment.getFileType())
                .fileSize(attachment.getFileSize())
                .uploadedBy(attachment.getUploadedBy())
                .uploadedAt(attachment.getUploadedAt())
                .build();
    }

    public LeadCustomFieldDto toCustomFieldDto(LeadCustomFieldDefinition field) {
        return LeadCustomFieldDto.builder()
                .id(field.getId())
                .tenantId(field.getTenantId())
                .fieldName(field.getFieldName())
                .fieldType(field.getFieldType())
                .fieldOptions(field.getFieldOptions())
                .required(field.isRequired())
                .displayOrder(field.getDisplayOrder())
                .createdAt(field.getCreatedAt())
                .build();
    }

    public LeadAttributionDto toAttributionDto(LeadAttribution attribution) {
        return LeadAttributionDto.builder()
                .id(attribution.getId())
                .leadId(attribution.getLeadId())
                .channel(attribution.getChannel())
                .campaign(attribution.getCampaign())
                .adId(attribution.getAdId())
                .position(attribution.getPosition())
                .cost(attribution.getCost())
                .sourceDetails(attribution.getSourceDetails())
                .createdAt(attribution.getCreatedAt())
                .build();
    }

    public LeadQualityScoreDto toQualityScoreDto(LeadQualityScore score) {
        return LeadQualityScoreDto.builder()
                .id(score.getId())
                .leadId(score.getLeadId())
                .overallScore(score.getOverallScore())
                .budgetFit(score.getBudgetFit())
                .timeline(score.getTimeline())
                .decisionMaker(score.getDecisionMaker())
                .competitorAwareness(score.getCompetitorAwareness())
                .objections(score.getObjections())
                .suggestedProduct(score.getSuggestedProduct())
                .nextAction(score.getNextAction())
                .rawLlmResponse(score.getRawLlmResponse())
                .scoredAt(score.getScoredAt())
                .build();
    }

    public AssignmentPoolMemberDto toPoolMemberDto(LeadAssignmentPoolMember member) {
        return AssignmentPoolMemberDto.builder()
                .id(member.getId())
                .tenantId(member.getTenantId())
                .userId(member.getUserId())
                .enabled(member.isEnabled())
                .lastAssignedAt(member.getLastAssignedAt())
                .build();
    }
}
