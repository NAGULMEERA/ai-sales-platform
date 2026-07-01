package com.aisales.common.contracts.feign;

import com.aisales.common.contracts.dto.tenant.TenantRequest;
import com.aisales.common.contracts.dto.tenant.TenantResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.UUID;

@FeignClient(name = "tenant-service", path = "/api/v1/tenants")
public interface TenantServiceClient {

    @PostMapping
    TenantResponse create(@RequestBody TenantRequest request);

    @GetMapping("/{id}")
    TenantResponse getById(@PathVariable UUID id);
}
