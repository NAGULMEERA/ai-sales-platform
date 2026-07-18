package com.aisales.identity.audit.application;

import com.aisales.common.core.util.CorrelationIdUtils;
import com.aisales.common.events.audit.AuditRecordedEvent;
import com.aisales.common.events.publisher.EventPublisher;
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
 * Persists immutable security audit records and publishes {@link AuditRecordedEvent}
 * via the outbox for downstream consumers. Callers must never pass passwords, refresh tokens,
 * reset tokens, or verification tokens in {@code detailsJson}.
 */
@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditLogRepository auditLogRepository;
    private final EventPublisher eventPublisher;

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
        String sanitizedDetails = sanitizeDetails(detailsJson);
        String correlationId = CorrelationIdUtils.get()
                .orElseGet(CorrelationIdUtils::generate);
        auditLogRepository.save(AuditLogEntry.builder()
                .tenantId(tenantId)
                .userId(userId)
                .action(action)
                .resourceType(resourceType)
                .resourceId(resourceId)
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .detailsJson(sanitizedDetails)
                .correlationId(correlationId)
                .createdAt(Instant.now())
                .build());
        publishAuditEvent(tenantId, userId, action, resourceType, resourceId, correlationId, sanitizedDetails);
    }

    private void publishAuditEvent(UUID tenantId, UUID userId, String action, String resourceType,
                                   String resourceId, String correlationId, String detailsJson) {
        String effectiveResourceId = resourceId != null
                ? resourceId
                : (userId != null ? userId.toString() : "unknown");
        String effectiveTenantId = tenantId != null ? tenantId.toString() : "platform";
        eventPublisher.publish(AuditRecordedEvent.of(
                effectiveTenantId,
                userId != null ? userId.toString() : null,
                action,
                resourceType,
                effectiveResourceId,
                correlationId,
                detailsJson));
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
