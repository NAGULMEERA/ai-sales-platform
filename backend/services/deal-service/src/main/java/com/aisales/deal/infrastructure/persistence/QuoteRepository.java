package com.aisales.deal.infrastructure.persistence;

import com.aisales.deal.domain.entity.Quote;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface QuoteRepository extends JpaRepository<Quote, UUID> {

    Optional<Quote> findByTenantIdAndIdAndDeletedAtIsNull(UUID tenantId, UUID id);

    List<Quote> findByTenantIdAndOpportunityIdAndDeletedAtIsNullOrderByQuoteVersionDesc(
            UUID tenantId, UUID opportunityId);

    @Query("""
            SELECT DISTINCT q FROM Quote q
            LEFT JOIN FETCH q.lineItems
            WHERE q.tenantId = :tenantId
              AND q.opportunityId = :opportunityId
              AND q.deletedAt IS NULL
            ORDER BY q.quoteVersion DESC
            """)
    List<Quote> findWithLineItemsByTenantIdAndOpportunityId(
            @Param("tenantId") UUID tenantId, @Param("opportunityId") UUID opportunityId);

    @Query("""
            SELECT q FROM Quote q
            LEFT JOIN FETCH q.lineItems
            WHERE q.tenantId = :tenantId
              AND q.id = :id
              AND q.deletedAt IS NULL
            """)
    Optional<Quote> findWithLineItemsByTenantIdAndId(
            @Param("tenantId") UUID tenantId, @Param("id") UUID id);

    @Query("""
            SELECT COALESCE(MAX(q.quoteVersion), 0)
            FROM Quote q
            WHERE q.tenantId = :tenantId
              AND q.opportunityId = :opportunityId
              AND q.deletedAt IS NULL
            """)
    int findMaxVersion(@Param("tenantId") UUID tenantId, @Param("opportunityId") UUID opportunityId);
}
