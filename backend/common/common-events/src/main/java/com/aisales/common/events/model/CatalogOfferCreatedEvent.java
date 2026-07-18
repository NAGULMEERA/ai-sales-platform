package com.aisales.common.events.model;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Getter
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class CatalogOfferCreatedEvent extends BaseEvent {

    private String productId;
    private String code;
    private String currency;
    private String unitPrice;
    private String status;

    public static CatalogOfferCreatedEvent of(String tenantId, String offerId, String productId,
                                              String code, String currency, String unitPrice,
                                              String status, String correlationId) {
        CatalogOfferCreatedEvent event = new CatalogOfferCreatedEvent();
        event.init("CatalogOfferCreated", tenantId, offerId, correlationId);
        event.productId = productId;
        event.code = code;
        event.currency = currency;
        event.unitPrice = unitPrice;
        event.status = status;
        return event;
    }
}
