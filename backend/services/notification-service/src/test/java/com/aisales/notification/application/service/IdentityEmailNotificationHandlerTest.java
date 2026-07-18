package com.aisales.notification.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

import com.aisales.common.contracts.notification.EmailTemplateCode;
import com.aisales.common.events.model.EmailVerificationRequestedEvent;
import com.aisales.common.events.model.PasswordResetRequestedEvent;
import com.aisales.notification.application.channel.NotificationDispatcher;
import com.aisales.notification.domain.channel.NotificationChannelType;
import com.aisales.notification.domain.channel.NotificationMessage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class IdentityEmailNotificationHandlerTest {

    @Mock
    private NotificationDispatcher notificationDispatcher;

    @InjectMocks
    private IdentityEmailNotificationHandler handler;

    @Test
    void shouldMapEmailVerificationEventToEmailChannel() {
        EmailVerificationRequestedEvent event = EmailVerificationRequestedEvent.of(
                "tenant-1", "user-1", "a@b.com", "Ada", "tok",
                "http://localhost/verify?token=tok", "corr-1");

        handler.onEmailVerificationRequested(event);

        ArgumentCaptor<NotificationMessage> captor = ArgumentCaptor.forClass(NotificationMessage.class);
        verify(notificationDispatcher).dispatch(eq(NotificationChannelType.EMAIL), captor.capture());
        NotificationMessage message = captor.getValue();
        assertThat(message.getTemplateCode()).isEqualTo(EmailTemplateCode.EMAIL_VERIFICATION);
        assertThat(message.getRecipient()).isEqualTo("a@b.com");
        assertThat(message.getVariables()).containsEntry("verificationLink",
                "http://localhost/verify?token=tok");
        assertThat(message.getCorrelationId()).isEqualTo("corr-1");
    }

    @Test
    void shouldMapPasswordResetEventToEmailChannel() {
        PasswordResetRequestedEvent event = PasswordResetRequestedEvent.of(
                "tenant-1", "user-1", "a@b.com", "Ada", "tok",
                "http://localhost/reset?token=tok", "corr-1");

        handler.onPasswordResetRequested(event);

        ArgumentCaptor<NotificationMessage> captor = ArgumentCaptor.forClass(NotificationMessage.class);
        verify(notificationDispatcher).dispatch(eq(NotificationChannelType.EMAIL), captor.capture());
        assertThat(captor.getValue().getTemplateCode()).isEqualTo(EmailTemplateCode.PASSWORD_RESET);
        assertThat(captor.getValue().getVariables()).containsEntry("resetLink",
                "http://localhost/reset?token=tok");
    }
}
