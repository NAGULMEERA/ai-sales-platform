package com.aisales.common.events.model;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Getter
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class CatalogProductCreatedEvent extends BaseEvent {

    private String code;
    private String name;
    private String productType;
    private String status;

    public static CatalogProductCreatedEvent of(String tenantId, String productId, String code,
                                                String name, String productType, String status,
                                                String correlationId) {
        CatalogProductCreatedEvent event = new CatalogProductCreatedEvent();
        event.init("CatalogProductCreated", tenantId, productId, correlationId);
        event.code = code;
        event.name = name;
        event.productType = productType;
        event.status = status;
        return event;
    }
}
