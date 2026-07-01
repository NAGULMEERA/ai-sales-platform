package com.aisales.common.contracts.client;

import com.aisales.common.core.dto.ApiResponse;
import com.aisales.common.core.dto.PageResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Map;
import java.util.UUID;

@FeignClient(name = "lead-service", path = "/api/v1/leads")
public interface LeadServiceClient {

    @GetMapping("/{id}")
    ApiResponse<Map<String, Object>> getLead(@PathVariable UUID id);

    @GetMapping
    ApiResponse<PageResponse<Map<String, Object>>> searchLeads(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size);
}
