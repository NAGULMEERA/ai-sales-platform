package com.aisales.identity.api.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class MessageResponse {

    private String message;
}
