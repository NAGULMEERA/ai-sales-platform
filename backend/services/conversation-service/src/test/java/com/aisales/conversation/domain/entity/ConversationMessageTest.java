package com.aisales.conversation.domain.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.aisales.common.contracts.conversation.MessageDeliveryStatus;
import com.aisales.common.exception.exception.ValidationException;
import org.junit.jupiter.api.Test;

class ConversationMessageTest {

    @Test
    void shouldTransitionPendingToSentToDeliveredToRead() {
        ConversationMessage message = ConversationMessage.builder()
                .deliveryStatus(MessageDeliveryStatus.PENDING)
                .retryCount(0)
                .build();
        message.transitionStatus(MessageDeliveryStatus.SENT, null);
        message.transitionStatus(MessageDeliveryStatus.DELIVERED, null);
        message.transitionStatus(MessageDeliveryStatus.READ, null);
        assertThat(message.getDeliveryStatus()).isEqualTo(MessageDeliveryStatus.READ);
        assertThat(message.getDeliveredAt()).isNotNull();
        assertThat(message.getReadAt()).isNotNull();
    }

    @Test
    void shouldRetryFailedMessage() {
        ConversationMessage message = ConversationMessage.builder()
                .deliveryStatus(MessageDeliveryStatus.FAILED)
                .retryCount(1)
                .failureReason("timeout")
                .build();
        message.markRetry();
        assertThat(message.getDeliveryStatus()).isEqualTo(MessageDeliveryStatus.PENDING);
        assertThat(message.getRetryCount()).isEqualTo(2);
        assertThat(message.getFailureReason()).isNull();
    }

    @Test
    void shouldRejectInvalidTransition() {
        ConversationMessage message = ConversationMessage.builder()
                .deliveryStatus(MessageDeliveryStatus.READ)
                .build();
        assertThatThrownBy(() -> message.transitionStatus(MessageDeliveryStatus.SENT, null))
                .isInstanceOf(ValidationException.class);
    }
}
