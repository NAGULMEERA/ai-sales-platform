package com.aisales.ai.infrastructure.persistence;

import com.aisales.ai.domain.entity.PromptTemplate;
import com.aisales.common.contracts.ai.PromptStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PromptTemplateRepository extends JpaRepository<PromptTemplate, UUID> {

    Optional<PromptTemplate> findByTenantIdAndIdAndDeletedAtIsNull(UUID tenantId, UUID id);

    Optional<PromptTemplate> findByTenantIdAndCodeAndDeletedAtIsNull(UUID tenantId, String code);

    Page<PromptTemplate> findByTenantIdAndDeletedAtIsNullOrderByUpdatedAtDesc(
            UUID tenantId, Pageable pageable);

    boolean existsByTenantIdAndCodeAndDeletedAtIsNull(UUID tenantId, String code);

    // industryCode/capability are always persisted upper-cased and languageCode lower-cased
    // (PromptService.normalizeDimension/normalizeLanguage runs on every write and every caller
    // of this method), so comparing the raw columns/params directly is equivalent to wrapping
    // both sides in UPPER(...) but lets the plain btree idx_prompt_template_dimensions
    // (tenant_id, industry_code, capability, language_code) index be used instead of forcing a
    // full scan (a function on an indexed column defeats a plain index).
    @Query("""
            SELECT p FROM PromptTemplate p
            WHERE p.tenantId = :tenantId
              AND p.deletedAt IS NULL
              AND p.status = :status
              AND p.industryCode = :industryCode
              AND p.capability = :capability
              AND (
                    (:languageCode IS NOT NULL AND p.languageCode = :languageCode)
                 OR (:languageCode IS NULL AND p.languageCode IS NULL)
              )
            ORDER BY p.updatedAt DESC
            """)
    List<PromptTemplate> findByDimensionsExact(
            @Param("tenantId") UUID tenantId,
            @Param("industryCode") String industryCode,
            @Param("capability") String capability,
            @Param("languageCode") String languageCode,
            @Param("status") PromptStatus status);

    @Query("""
            SELECT p FROM PromptTemplate p
            WHERE p.tenantId = :tenantId
              AND p.deletedAt IS NULL
              AND p.status = :status
              AND p.industryCode = :industryCode
              AND p.capability = :capability
              AND p.languageCode IS NULL
            ORDER BY p.updatedAt DESC
            """)
    List<PromptTemplate> findByDimensionsLanguageAgnostic(
            @Param("tenantId") UUID tenantId,
            @Param("industryCode") String industryCode,
            @Param("capability") String capability,
            @Param("status") PromptStatus status);
}
