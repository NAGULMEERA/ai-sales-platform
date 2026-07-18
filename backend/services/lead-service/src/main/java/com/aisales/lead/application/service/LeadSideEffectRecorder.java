package com.aisales.lead.application.service;

import com.aisales.common.contracts.lead.LeadStatus;
import com.aisales.lead.domain.entity.LeadActivity;
import com.aisales.lead.domain.entity.LeadStatusHistory;
import com.aisales.lead.infrastructure.persistence.LeadActivityRepository;
import com.aisales.lead.infrastructure.persistence.LeadStatusHistoryRepository;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class LeadSideEffectRecorder {

    private final LeadStatusHistoryRepository statusHistoryRepository;
    private final LeadActivityRepository activityRepository;

    public void recordStatusChange(UUID leadId, LeadStatus oldStatus, LeadStatus newStatus,
                                   String reason, UUID actor) {
        if (oldStatus == newStatus) {
            return;
        }
        statusHistoryRepository.save(LeadStatusHistory.builder()
                .leadId(leadId)
                .oldStatus(oldStatus)
                .newStatus(newStatus)
                .reason(reason)
                .changedBy(actor)
                .createdAt(Instant.now())
                .build());
        recordActivity(leadId, "STATUS_CHANGED",
                "Status " + oldStatus + " → " + newStatus
                        + (reason != null ? " (" + reason + ")" : ""),
                actor);
    }

    public void recordActivity(UUID leadId, String type, String description, UUID actor) {
        activityRepository.save(LeadActivity.builder()
                .leadId(leadId)
                .activityType(type)
                .description(description)
                .createdBy(actor)
                .createdAt(Instant.now())
                .build());
    }
}
