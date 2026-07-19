package com.aisales.common.contracts.conversation;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateMessageStatusRequest {

    @NotNull
    private MessageDeliveryStatus status;

    @Size(max = 1000)
    private String failureReason;
}
