package com.aisales.identity.application.service;

import com.aisales.common.core.util.CorrelationIdUtils;
import com.aisales.identity.domain.entity.AuditLogEntry;
import com.aisales.identity.infrastructure.persistence.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    @Transactional
    public void log(UUID tenantId, UUID userId, String action, String resourceType, String resourceId,
                    String ipAddress, String userAgent, String detailsJson) {
        auditLogRepository.save(AuditLogEntry.builder()
                .tenantId(tenantId)
                .userId(userId)
                .action(action)
                .resourceType(resourceType)
                .resourceId(resourceId)
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .detailsJson(detailsJson)
                .correlationId(CorrelationIdUtils.getCorrelationId())
                .createdAt(Instant.now())
                .build());
    }
}
