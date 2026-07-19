package com.aisales.search.infrastructure.persistence;

import com.aisales.common.contracts.search.SearchEntityType;
import com.aisales.search.domain.entity.SearchDocument;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SearchDocumentRepository extends JpaRepository<SearchDocument, UUID> {

    Optional<SearchDocument> findByTenantIdAndEntityTypeAndEntityIdAndDeletedAtIsNull(
            UUID tenantId, SearchEntityType entityType, UUID entityId);

    @Modifying
    @Query(value = """
            UPDATE search_document
            SET search_vector =
                setweight(to_tsvector('english', coalesce(title, '')), 'A') ||
                setweight(to_tsvector('english', coalesce(keywords, '')), 'B') ||
                setweight(to_tsvector('english', coalesce(body, '')), 'C'),
                updated_at = NOW()
            WHERE id = :id
            """, nativeQuery = true)
    void refreshSearchVector(@Param("id") UUID id);

    @Query(value = """
            SELECT d.id
            FROM search_document d
            WHERE d.tenant_id = :tenantId
              AND d.deleted_at IS NULL
              AND (:entityType = 'ALL' OR d.entity_type = :entityType)
              AND (:status IS NULL OR d.status = :status)
              AND (:source IS NULL OR d.source = :source)
              AND (
                    :query IS NULL OR :query = ''
                    OR d.search_vector @@ plainto_tsquery('english', :query)
                    OR lower(d.title) LIKE lower(concat(:query, '%'))
                    OR lower(d.title) LIKE lower(concat('%', :query, '%'))
                  )
            ORDER BY
              CASE WHEN :query IS NULL OR :query = '' THEN 0
                   ELSE ts_rank(d.search_vector, plainto_tsquery('english', :query))
              END * 0.45
              + LEAST(COALESCE(d.business_score, 0) / 100.0, 1.0) * 0.25
              + LEAST(COALESCE(d.popularity, 0) / 1000.0, 1.0) * 0.15
              + CASE
                    WHEN d.source_updated_at IS NULL THEN 0
                    ELSE GREATEST(0, 1.0 - EXTRACT(EPOCH FROM (NOW() - d.source_updated_at)) / 2592000.0)
                END * 0.15
              DESC,
              d.source_updated_at DESC NULLS LAST
            LIMIT :limit OFFSET :offset
            """, nativeQuery = true)
    List<UUID> searchHybrid(
            @Param("tenantId") UUID tenantId,
            @Param("entityType") String entityType,
            @Param("query") String query,
            @Param("status") String status,
            @Param("source") String source,
            @Param("limit") int limit,
            @Param("offset") int offset);

    @Query(value = """
            SELECT COUNT(*)
            FROM search_document d
            WHERE d.tenant_id = :tenantId
              AND d.deleted_at IS NULL
              AND (:entityType = 'ALL' OR d.entity_type = :entityType)
              AND (:status IS NULL OR d.status = :status)
              AND (:source IS NULL OR d.source = :source)
              AND (
                    :query IS NULL OR :query = ''
                    OR d.search_vector @@ plainto_tsquery('english', :query)
                    OR lower(d.title) LIKE lower(concat(:query, '%'))
                    OR lower(d.title) LIKE lower(concat('%', :query, '%'))
                  )
            """, nativeQuery = true)
    long countHybrid(
            @Param("tenantId") UUID tenantId,
            @Param("entityType") String entityType,
            @Param("query") String query,
            @Param("status") String status,
            @Param("source") String source);

    @Query(value = """
            SELECT d.id
            FROM search_document d
            WHERE d.tenant_id = :tenantId
              AND d.deleted_at IS NULL
              AND d.embedding IS NOT NULL
              AND (:entityType = 'ALL' OR d.entity_type = :entityType)
              AND (:status IS NULL OR d.status = :status)
            ORDER BY d.embedding <=> cast(:embedding as vector)
            LIMIT :limit OFFSET :offset
            """, nativeQuery = true)
    List<UUID> searchVector(
            @Param("tenantId") UUID tenantId,
            @Param("entityType") String entityType,
            @Param("status") String status,
            @Param("embedding") String embeddingLiteral,
            @Param("limit") int limit,
            @Param("offset") int offset);

    @Query("""
            SELECT d FROM SearchDocument d
            WHERE d.tenantId = :tenantId AND d.deletedAt IS NULL AND d.id IN :ids
            """)
    List<SearchDocument> findAllByTenantAndIds(
            @Param("tenantId") UUID tenantId, @Param("ids") List<UUID> ids);

    @Query(value = """
            SELECT DISTINCT d.title
            FROM search_document d
            WHERE d.tenant_id = :tenantId
              AND d.deleted_at IS NULL
              AND (:entityType = 'ALL' OR d.entity_type = :entityType)
              AND lower(d.title) LIKE lower(concat(:prefix, '%'))
            ORDER BY d.title
            LIMIT :limit
            """, nativeQuery = true)
    List<String> autocomplete(
            @Param("tenantId") UUID tenantId,
            @Param("entityType") String entityType,
            @Param("prefix") String prefix,
            @Param("limit") int limit);

    @Query(value = """
            SELECT d.status, COUNT(*)
            FROM search_document d
            WHERE d.tenant_id = :tenantId
              AND d.deleted_at IS NULL
              AND (:entityType = 'ALL' OR d.entity_type = :entityType)
              AND d.status IS NOT NULL
            GROUP BY d.status
            """, nativeQuery = true)
    List<Object[]> facetStatus(
            @Param("tenantId") UUID tenantId, @Param("entityType") String entityType);

    @Query(value = """
            SELECT d.entity_type, COUNT(*)
            FROM search_document d
            WHERE d.tenant_id = :tenantId
              AND d.deleted_at IS NULL
            GROUP BY d.entity_type
            """, nativeQuery = true)
    List<Object[]> facetEntityType(@Param("tenantId") UUID tenantId);

    @Query(value = """
            SELECT d.source, COUNT(*)
            FROM search_document d
            WHERE d.tenant_id = :tenantId
              AND d.deleted_at IS NULL
              AND (:entityType = 'ALL' OR d.entity_type = :entityType)
              AND d.source IS NOT NULL
            GROUP BY d.source
            """, nativeQuery = true)
    List<Object[]> facetSource(
            @Param("tenantId") UUID tenantId, @Param("entityType") String entityType);
}
