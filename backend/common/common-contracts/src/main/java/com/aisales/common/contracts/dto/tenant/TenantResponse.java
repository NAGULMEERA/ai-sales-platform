package com.aisales.common.contracts.dto.tenant;

import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class TenantResponse {

    private UUID id;
    private String name;
    private String slug;
    private boolean active;
}
