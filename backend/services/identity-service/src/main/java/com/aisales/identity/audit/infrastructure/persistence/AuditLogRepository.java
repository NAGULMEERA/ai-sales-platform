package com.aisales.identity.audit.infrastructure.persistence;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import com.aisales.identity.audit.domain.entity.AuditLogEntry;



public interface AuditLogRepository extends JpaRepository<AuditLogEntry, UUID> {

    long countByAction(String action);

    long countByActionAndUserId(String action, UUID userId);
}
