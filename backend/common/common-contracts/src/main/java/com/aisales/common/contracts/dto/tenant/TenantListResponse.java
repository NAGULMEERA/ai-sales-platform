package com.aisales.common.contracts.dto.tenant;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class TenantListResponse {

    private List<TenantResponse> tenants;
    private long total;
}
