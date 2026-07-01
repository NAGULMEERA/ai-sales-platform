package com.aisales.identity.api.response;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
public class SessionResponse {

    private UUID sessionId;
    private String deviceLabel;
    private String ipAddress;
    private Instant createdAt;
    private Instant expiresAt;
    private boolean current;
}
