package com.aisales.common.contracts.conversation;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
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
}
