package com.aisales.common.events.event;

import lombok.Getter;
import lombok.experimental.SuperBuilder;

@Getter
@SuperBuilder
public class CustomerMergedEvent extends BaseEvent {

    private String sourceId;
    private String targetId;
}
