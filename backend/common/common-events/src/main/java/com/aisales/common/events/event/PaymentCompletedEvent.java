package com.aisales.common.events.event;

import lombok.Getter;
import lombok.experimental.SuperBuilder;

@Getter
@SuperBuilder
public class PaymentCompletedEvent extends BaseEvent {

    private String paymentId;
    private String amount;
    private String currency;
}
