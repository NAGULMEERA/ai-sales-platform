package com.aisales.common.events.event;

import lombok.Getter;
import lombok.experimental.SuperBuilder;

@Getter
@SuperBuilder
public class UserCreatedEvent extends BaseEvent {

    private String userId;
    private String email;
}
