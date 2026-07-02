package com.aisales.tenant.infrastructure.audit;

import com.aisales.common.core.audit.AuditRecord;
import com.aisales.common.core.audit.AuditRecorder;
import com.aisales.tenant.domain.entity.TenantAuditLog;
import com.aisales.tenant.infrastructure.persistence.TenantAuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
@RequiredArgsConstructor
public class TenantAuditRecorder implements AuditRecorder {

    private final TenantAuditLogRepository tenantAuditLogRepository;

    @Override
    public void record(AuditRecord record) {
        if (record.getResourceId() == null) {
            return;
        }
        tenantAuditLogRepository.save(TenantAuditLog.builder()
                .tenantId(record.getTenantId())
                .userId(record.getUserId())
                .action(record.getAction())
                .resourceType(record.getResourceType())
                .resourceId(record.getResourceId())
                .correlationId(record.getCorrelationId())
                .detailsJson(record.getDetailsJson())
                .createdAt(Instant.now())
                .build());
    }
}
