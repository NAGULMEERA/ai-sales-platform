package com.aisales.common.core.audit;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;

/**
 * Default audit sink — structured log line suitable for log aggregation until audit-service consumes events.
 */
@Slf4j
public class LoggingAuditRecorder implements AuditRecorder {

    @Override
    public void record(AuditRecord record) {
        if (record.getResourceId() == null) {
            return;
        }
        MDC.put("audit_action", record.getAction());
        MDC.put("audit_resource_type", record.getResourceType());
        MDC.put("audit_resource_id", record.getResourceId());
        try {
            log.info(
                    "audit_event tenant_id={} user_id={} action={} resource_type={} resource_id={} correlation_id={}",
                    record.getTenantId(),
                    record.getUserId(),
                    record.getAction(),
                    record.getResourceType(),
                    record.getResourceId(),
                    record.getCorrelationId());
        } finally {
            MDC.remove("audit_action");
            MDC.remove("audit_resource_type");
            MDC.remove("audit_resource_id");
        }
    }
}
