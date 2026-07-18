package com.aisales.notification.application.service;

import com.aisales.common.contracts.notification.EmailTemplateCode;
import com.aisales.common.contracts.notification.SendTransactionalEmailRequest;
import com.aisales.common.events.model.EmailVerificationRequestedEvent;
import com.aisales.common.events.model.PasswordResetRequestedEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class IdentityEmailNotificationHandlerTest {

    @Mock
    private EmailDeliveryService emailDeliveryService;

    @InjectMocks
    private IdentityEmailNotificationHandler handler;

    @Test
    void shouldMapEmailVerificationEventToTemplateRequest() {
        EmailVerificationRequestedEvent event = EmailVerificationRequestedEvent.of(
                "tenant-1", "user-1", "a@b.com", "Ada", "tok",
                "http://localhost/verify?token=tok", "corr-1");

        handler.onEmailVerificationRequested(event);

        ArgumentCaptor<SendTransactionalEmailRequest> captor =
                ArgumentCaptor.forClass(SendTransactionalEmailRequest.class);
        verify(emailDeliveryService).sendTransactionalEmail(captor.capture());
        SendTransactionalEmailRequest request = captor.getValue();
        assertThat(request.getTemplateCode()).isEqualTo(EmailTemplateCode.EMAIL_VERIFICATION);
        assertThat(request.getRecipientEmail()).isEqualTo("a@b.com");
        assertThat(request.getVariables()).containsEntry("verificationLink",
                "http://localhost/verify?token=tok");
        assertThat(request.getCorrelationId()).isEqualTo("corr-1");
    }

    @Test
    void shouldMapPasswordResetEventToTemplateRequest() {
        PasswordResetRequestedEvent event = PasswordResetRequestedEvent.of(
                "tenant-1", "user-1", "a@b.com", "Ada", "tok",
                "http://localhost/reset?token=tok", "corr-1");

        handler.onPasswordResetRequested(event);

        ArgumentCaptor<SendTransactionalEmailRequest> captor =
                ArgumentCaptor.forClass(SendTransactionalEmailRequest.class);
        verify(emailDeliveryService).sendTransactionalEmail(captor.capture());
        assertThat(captor.getValue().getTemplateCode()).isEqualTo(EmailTemplateCode.PASSWORD_RESET);
        assertThat(captor.getValue().getVariables()).containsEntry("resetLink",
                "http://localhost/reset?token=tok");
    }
}
