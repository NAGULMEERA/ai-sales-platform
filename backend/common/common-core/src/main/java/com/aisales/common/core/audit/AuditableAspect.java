package com.aisales.common.core.audit;

import com.aisales.common.core.util.CorrelationIdUtils;
import com.aisales.common.core.util.TenantContext;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Aspect
@Component
@RequiredArgsConstructor
@ConditionalOnBean(AuditRecorder.class)
public class AuditableAspect {

    private final AuditRecorder auditRecorder;

    @AfterReturning(pointcut = "@annotation(auditable)", returning = "result")
    public void recordAudit(JoinPoint joinPoint, Auditable auditable, Object result) {
        String resourceId = resolveResourceId(result, joinPoint.getArgs());
        UUID tenantId = TenantContext.getTenantIdAsUuid().orElse(null);
        auditRecorder.record(AuditRecord.builder()
                .tenantId(tenantId)
                .userId(TenantContext.getUserId())
                .action(auditable.action())
                .resourceType(auditable.resourceType())
                .resourceId(resourceId)
                .correlationId(CorrelationIdUtils.getCorrelationId())
                .build());
    }

    private String resolveResourceId(Object result, Object[] args) {
        String fromResult = extractId(result);
        if (fromResult != null) {
            return fromResult;
        }
        if (args != null) {
            for (Object arg : args) {
                if (arg instanceof UUID uuid) {
                    return uuid.toString();
                }
                String fromArg = extractId(arg);
                if (fromArg != null) {
                    return fromArg;
                }
            }
        }
        return null;
    }

    private String extractId(Object value) {
        if (value == null) {
            return null;
        }
        try {
            var method = value.getClass().getMethod("getId");
            Object id = method.invoke(value);
            return id != null ? id.toString() : null;
        } catch (ReflectiveOperationException ex) {
            return null;
        }
    }
}
