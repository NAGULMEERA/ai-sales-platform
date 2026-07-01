package com.aisales.identity.infrastructure.persistence;

import com.aisales.identity.domain.entity.AuditLogEntry;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface AuditLogRepository extends JpaRepository<AuditLogEntry, UUID> {
}
