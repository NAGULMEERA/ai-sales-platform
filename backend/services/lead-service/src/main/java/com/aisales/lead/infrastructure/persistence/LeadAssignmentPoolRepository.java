package com.aisales.lead.infrastructure.persistence;

import com.aisales.lead.domain.entity.LeadAssignmentPoolMember;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LeadAssignmentPoolRepository extends JpaRepository<LeadAssignmentPoolMember, UUID> {

    List<LeadAssignmentPoolMember> findByTenantIdOrderByCreatedAtAsc(UUID tenantId);

    Optional<LeadAssignmentPoolMember> findByTenantIdAndUserId(UUID tenantId, UUID userId);

    @Query("""
            SELECT m FROM LeadAssignmentPoolMember m
            WHERE m.tenantId = :tenantId AND m.enabled = true
            ORDER BY m.lastAssignedAt ASC NULLS FIRST, m.createdAt ASC
            """)
    List<LeadAssignmentPoolMember> findEnabledForRoundRobin(@Param("tenantId") UUID tenantId);
}
