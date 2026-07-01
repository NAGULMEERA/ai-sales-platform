package com.aisales.common.events.publisher;

import com.aisales.common.events.model.BaseEvent;

public interface EventPublisher {

    void publish(BaseEvent event);

    void publish(String topic, BaseEvent event);
}
