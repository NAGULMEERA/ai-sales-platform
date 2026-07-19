package com.aisales.common.contracts.conversation;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AddMessageRequest {

    @NotNull
    private MessageSenderType senderType;

    private UUID senderId;

    @NotBlank
    @Size(max = 8000)
    private String body;

    /** When null, inferred from senderType (CUSTOMER → INBOUND, others → OUTBOUND). */
    private MessageDirection direction;

    @Builder.Default
    private MessageContentType contentType = MessageContentType.TEXT;

    @Size(max = 255)
    private String correlationId;

    private UUID mediaId;

    @Size(max = 255)
    private String mediaUrl;

    @Size(max = 128)
    private String mediaContentType;

    @Builder.Default
    private List<UUID> attachmentMediaIds = new ArrayList<>();
}
