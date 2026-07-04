package com.aisales.identity.audit.application;

import com.aisales.common.core.util.CorrelationIdUtils;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import com.aisales.identity.audit.domain.AuditAction;
import com.aisales.identity.audit.domain.entity.AuditLogEntry;
import com.aisales.identity.audit.infrastructure.persistence.AuditLogRepository;



/**
 * Persists immutable security audit records. Callers must never pass passwords, refresh tokens,
 * reset tokens, or verification tokens in {@code detailsJson}.
 */
@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    @Transactional
    public void logSecurityEvent(UUID tenantId, UUID userId, AuditAction action, String resourceType,
                                 String resourceId, String ipAddress, String userAgent, String detailsJson) {
        log(tenantId, userId, action.name(), resourceType, resourceId, ipAddress, userAgent, detailsJson);
    }

    /**
     * Persists audit records that must survive rollback of the surrounding business transaction
     * (e.g. failed login attempts).
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logSecurityEventIndependent(UUID tenantId, UUID userId, AuditAction action, String resourceType,
                                            String resourceId, String ipAddress, String userAgent, String detailsJson) {
        log(tenantId, userId, action.name(), resourceType, resourceId, ipAddress, userAgent, detailsJson);
    }

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
                .detailsJson(sanitizeDetails(detailsJson))
                .correlationId(CorrelationIdUtils.getCorrelationId())
                .createdAt(Instant.now())
                .build());
    }

    private static String sanitizeDetails(String detailsJson) {
        if (detailsJson == null) {
            return null;
        }
        String lower = detailsJson.toLowerCase();
        if (lower.contains("password") || lower.contains("token") || lower.contains("secret")) {
            throw new IllegalArgumentException("Audit details must not contain sensitive field names");
        }
        return detailsJson;
    }
}
