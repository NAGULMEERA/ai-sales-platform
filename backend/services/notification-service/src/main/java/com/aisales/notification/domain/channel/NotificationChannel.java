package com.aisales.notification.domain.channel;

/**
 * Extension point for delivery channels. Email is implemented today;
 * WhatsApp/SMS/Push can be added without changing identity consumers.
 */
public interface NotificationChannel {

    NotificationChannelType type();

    void send(NotificationMessage message);
}
