package com.aisales.lead.application.service;

import com.aisales.common.contracts.lead.LeadStatus;
import com.aisales.lead.domain.entity.LeadActivity;
import com.aisales.lead.domain.entity.LeadStatusHistory;
import com.aisales.lead.infrastructure.persistence.LeadActivityRepository;
import com.aisales.lead.infrastructure.persistence.LeadStatusHistoryRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

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

    /**
     * Persists multiple activities for the same lead as a single atomic unit.
     * Callers that record several related audit entries after an external operation
     * completes (e.g. opportunity conversion) should use this instead of issuing
     * separate {@link #recordActivity} calls, each of which auto-commits independently
     * and could leave a partial audit trail if the process fails mid-sequence.
     */
    @Transactional
    public void recordActivities(UUID leadId, UUID actor, List<ActivityEntry> entries) {
        if (entries == null || entries.isEmpty()) {
            return;
        }
        Instant now = Instant.now();
        List<LeadActivity> activities = entries.stream()
                .map(entry -> LeadActivity.builder()
                        .leadId(leadId)
                        .activityType(entry.type())
                        .description(entry.description())
                        .createdBy(actor)
                        .createdAt(now)
                        .build())
                .toList();
        activityRepository.saveAll(activities);
    }

    public record ActivityEntry(String type, String description) {}
}
