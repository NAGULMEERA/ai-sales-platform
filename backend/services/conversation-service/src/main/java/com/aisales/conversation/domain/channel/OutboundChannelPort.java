package com.aisales.conversation.domain.channel;

import com.aisales.common.contracts.conversation.ConversationChannel;
import com.aisales.conversation.domain.entity.ConversationMessage;
import com.aisales.conversation.domain.entity.ConversationThread;

/**
 * Channel-agnostic outbound delivery port. Provider plugins (WhatsApp, SMS, Email, Voice,
 * Social) implement this without coupling the conversation aggregate to a vendor SDK.
 */
public interface OutboundChannelPort {

    boolean supports(ConversationChannel channel);

    void dispatch(ConversationThread thread, ConversationMessage message);
}
