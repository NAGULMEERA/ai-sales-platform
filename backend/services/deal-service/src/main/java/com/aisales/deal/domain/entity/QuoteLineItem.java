package com.aisales.deal.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "quote_line_item")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuoteLineItem {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "quote_id", nullable = false)
    private Quote quote;

    @Column(name = "product_id")
    private UUID productId;

    @Column(name = "offer_id")
    private UUID offerId;

    @Column(nullable = false, length = 100)
    private String code;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal quantity;

    @Column(name = "unit_price", nullable = false, precision = 19, scale = 4)
    private BigDecimal unitPrice;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(name = "line_total", nullable = false, precision = 19, scale = 4)
    private BigDecimal lineTotal;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public static QuoteLineItem of(UUID productId, UUID offerId, String code, String name,
                                   BigDecimal quantity, BigDecimal unitPrice, String currency) {
        BigDecimal qty = quantity.setScale(4, RoundingMode.HALF_UP);
        BigDecimal price = unitPrice.setScale(4, RoundingMode.HALF_UP);
        return QuoteLineItem.builder()
                .productId(productId)
                .offerId(offerId)
                .code(code)
                .name(name)
                .quantity(qty)
                .unitPrice(price)
                .currency(currency)
                .lineTotal(qty.multiply(price).setScale(4, RoundingMode.HALF_UP))
                .createdAt(Instant.now())
                .build();
    }
}
