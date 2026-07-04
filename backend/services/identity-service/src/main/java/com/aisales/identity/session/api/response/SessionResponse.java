package com.aisales.identity.session.api.response;

import java.time.Instant;
import java.util.UUID;
import lombok.Builder;
import lombok.Data;



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
