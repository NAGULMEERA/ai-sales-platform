package com.aisales.lead.infrastructure.persistence;

import com.aisales.common.contracts.lead.LeadStatus;
import com.aisales.lead.domain.entity.Lead;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LeadRepository extends JpaRepository<Lead, UUID> {

    Optional<Lead> findByTenantIdAndIdAndDeletedAtIsNull(UUID tenantId, UUID id);

    Optional<Lead> findByTenantIdAndId(UUID tenantId, UUID id);

    Page<Lead> findByTenantIdAndDeletedAtIsNullOrderByCreatedAtDesc(UUID tenantId, Pageable pageable);

    @Query("""
            SELECT l FROM Lead l
            WHERE l.tenantId = :tenantId
              AND l.deletedAt IS NULL
              AND (:status IS NULL OR l.status = :status)
              AND (:assignedTo IS NULL OR l.assignedTo = :assignedTo)
              AND (:sourceType IS NULL OR l.sourceType = :sourceType)
              AND (
                    :q IS NULL OR :q = ''
                    OR LOWER(l.customerName) LIKE LOWER(CONCAT('%', :q, '%'))
                    OR LOWER(l.phone) LIKE LOWER(CONCAT('%', :q, '%'))
                    OR LOWER(COALESCE(l.email, '')) LIKE LOWER(CONCAT('%', :q, '%'))
                  )
            ORDER BY l.createdAt DESC
            """)
    Page<Lead> search(
            @Param("tenantId") UUID tenantId,
            @Param("status") LeadStatus status,
            @Param("assignedTo") UUID assignedTo,
            @Param("sourceType") String sourceType,
            @Param("q") String q,
            Pageable pageable);

    @Query("""
            SELECT DISTINCT l FROM Lead l
            WHERE l.tenantId = :tenantId
              AND l.deletedAt IS NULL
              AND l.id <> :leadId
              AND (
                    (:phone IS NOT NULL AND l.phone = :phone)
                    OR (:email IS NOT NULL AND LOWER(l.email) = LOWER(:email))
                    OR (:whatsapp IS NOT NULL AND l.phone = :whatsapp)
                    OR (
                         :sourceType IS NOT NULL AND :sourceId IS NOT NULL
                         AND l.sourceType = :sourceType
                         AND l.sourceId = :sourceId
                       )
                  )
            """)
    List<Lead> findPotentialDuplicates(
            @Param("tenantId") UUID tenantId,
            @Param("leadId") UUID leadId,
            @Param("phone") String phone,
            @Param("email") String email,
            @Param("whatsapp") String whatsapp,
            @Param("sourceType") String sourceType,
            @Param("sourceId") String sourceId);
}
