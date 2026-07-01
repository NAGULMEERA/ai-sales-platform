package com.aisales.common.contracts.notification;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SendTransactionalEmailRequest {

    private String tenantId;

    @NotBlank
    @Email
    private String recipientEmail;

    @NotBlank
    private String templateCode;

    private Map<String, String> variables;

    private String correlationId;
}
