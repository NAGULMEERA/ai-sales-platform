package com.aisales.notification.application.channel;

import com.aisales.common.exception.exception.ValidationException;
import com.aisales.notification.domain.channel.NotificationChannel;
import com.aisales.notification.domain.channel.NotificationChannelType;
import com.aisales.notification.domain.channel.NotificationMessage;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class NotificationDispatcher {

    private final Map<NotificationChannelType, NotificationChannel> channels;

    public NotificationDispatcher(List<NotificationChannel> channelList) {
        Map<NotificationChannelType, NotificationChannel> map = new EnumMap<>(NotificationChannelType.class);
        for (NotificationChannel channel : channelList) {
            map.put(channel.type(), channel);
        }
        this.channels = Map.copyOf(map);
    }

    public void dispatch(NotificationChannelType type, NotificationMessage message) {
        NotificationChannel channel = channels.get(type);
        if (channel == null) {
            throw new ValidationException("Notification channel not configured: " + type);
        }
        channel.send(message);
    }
}
