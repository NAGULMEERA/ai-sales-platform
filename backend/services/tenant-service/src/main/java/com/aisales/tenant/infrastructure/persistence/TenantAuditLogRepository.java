package com.aisales.tenant.infrastructure.persistence;

import com.aisales.tenant.domain.entity.TenantAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface TenantAuditLogRepository extends JpaRepository<TenantAuditLog, UUID> {
}
