package com.aisales.conversation.infrastructure.channel;

import com.aisales.common.contracts.conversation.ConversationChannel;
import com.aisales.conversation.domain.channel.OutboundChannelPort;
import com.aisales.conversation.domain.entity.ConversationMessage;
import com.aisales.conversation.domain.entity.ConversationThread;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Default outbound adapter for omnichannel foundation.
 * Capability plugins (WhatsApp, SMS, Email, Voice, Social) replace or decorate this port.
 * ConversationService owns MessageSent integration events; this adapter never publishes them.
 */
@Slf4j
@Component
public class EventOutboundChannelAdapter implements OutboundChannelPort {

    @Override
    public boolean supports(ConversationChannel channel) {
        return channel != null;
    }

    @Override
    public void dispatch(ConversationThread thread, ConversationMessage message) {
        log.info(
                "Outbound channel dispatch queued tenant={} conversation={} channel={} message={}",
                thread.getTenantId(),
                thread.getId(),
                thread.getChannel(),
                message.getId());
    }
}
