package com.aisales.common.contracts.conversation;

import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConversationAttachmentDto {

    private UUID id;
    private UUID messageId;
    private UUID mediaId;
    private String fileName;
    private String contentType;
    private Long sizeBytes;
    private Instant createdAt;
}
