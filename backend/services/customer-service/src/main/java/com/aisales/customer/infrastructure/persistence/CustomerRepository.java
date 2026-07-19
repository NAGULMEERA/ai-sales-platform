package com.aisales.customer.infrastructure.persistence;

import com.aisales.common.contracts.customer.CustomerStatus;
import com.aisales.customer.domain.entity.Customer;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CustomerRepository extends JpaRepository<Customer, UUID> {

    Optional<Customer> findByTenantIdAndIdAndDeletedAtIsNull(UUID tenantId, UUID id);

    Optional<Customer> findByTenantIdAndId(UUID tenantId, UUID id);

    Optional<Customer> findByTenantIdAndSourceLeadIdAndDeletedAtIsNull(UUID tenantId, UUID sourceLeadId);

    @Query("""
            SELECT c FROM Customer c
            WHERE c.tenantId = :tenantId
              AND c.deletedAt IS NULL
              AND (:status IS NULL OR c.status = :status)
              AND (
                    :q IS NULL OR :q = ''
                    OR LOWER(c.fullName) LIKE LOWER(CONCAT('%', :q, '%'))
                    OR LOWER(COALESCE(c.email, '')) LIKE LOWER(CONCAT('%', :q, '%'))
                    OR COALESCE(c.phone, '') LIKE CONCAT('%', :q, '%')
                    OR COALESCE(c.whatsapp, '') LIKE CONCAT('%', :q, '%')
                    OR LOWER(COALESCE(c.externalCrmId, '')) LIKE LOWER(CONCAT('%', :q, '%'))
                    OR LOWER(COALESCE(c.customerNumber, '')) LIKE LOWER(CONCAT('%', :q, '%'))
                  )
            ORDER BY c.createdAt DESC
            """)
    Page<Customer> search(
            @Param("tenantId") UUID tenantId,
            @Param("status") CustomerStatus status,
            @Param("q") String q,
            Pageable pageable);

    @Query("""
            SELECT DISTINCT c FROM Customer c
            WHERE c.tenantId = :tenantId
              AND c.deletedAt IS NULL
              AND (
                    (:phone IS NOT NULL AND c.phone = :phone)
                    OR (:email IS NOT NULL AND LOWER(c.email) = LOWER(:email))
                    OR (:whatsapp IS NOT NULL AND c.whatsapp = :whatsapp)
                    OR (:externalCrmId IS NOT NULL AND c.externalCrmId = :externalCrmId)
                    OR (:governmentId IS NOT NULL AND c.governmentId = :governmentId)
                    OR (:customerNumber IS NOT NULL AND c.customerNumber = :customerNumber)
                  )
            """)
    List<Customer> findIdentityCandidates(
            @Param("tenantId") UUID tenantId,
            @Param("phone") String phone,
            @Param("email") String email,
            @Param("whatsapp") String whatsapp,
            @Param("externalCrmId") String externalCrmId,
            @Param("governmentId") String governmentId,
            @Param("customerNumber") String customerNumber);

    @Query("""
            SELECT DISTINCT c FROM Customer c
            WHERE c.tenantId = :tenantId
              AND c.deletedAt IS NULL
              AND c.id <> :customerId
              AND (
                    (:phone IS NOT NULL AND c.phone = :phone)
                    OR (:email IS NOT NULL AND LOWER(c.email) = LOWER(:email))
                    OR (:whatsapp IS NOT NULL AND c.whatsapp = :whatsapp)
                    OR (:externalCrmId IS NOT NULL AND c.externalCrmId = :externalCrmId)
                    OR (:governmentId IS NOT NULL AND c.governmentId = :governmentId)
                  )
            """)
    List<Customer> findPotentialDuplicates(
            @Param("tenantId") UUID tenantId,
            @Param("customerId") UUID customerId,
            @Param("phone") String phone,
            @Param("email") String email,
            @Param("whatsapp") String whatsapp,
            @Param("externalCrmId") String externalCrmId,
            @Param("governmentId") String governmentId);
}
