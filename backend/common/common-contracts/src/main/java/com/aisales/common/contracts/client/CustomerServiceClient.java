package com.aisales.common.contracts.client;

import com.aisales.common.contracts.customer.CreateCustomerRequest;
import com.aisales.common.contracts.customer.CustomerDto;
import com.aisales.common.core.dto.ApiResponse;
import com.aisales.common.core.dto.PageResponse;
import java.util.UUID;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(
        name = "customer-service",
        path = "/api/v1/customers",
        url = "${aisales.clients.customer-service.url:}")
public interface CustomerServiceClient {

    @PostMapping
    ApiResponse<CustomerDto> createCustomer(@RequestBody CreateCustomerRequest request);

    @GetMapping("/{id}")
    ApiResponse<CustomerDto> getCustomer(@PathVariable UUID id);

    @GetMapping("/by-source-lead/{leadId}")
    ApiResponse<CustomerDto> getBySourceLead(@PathVariable UUID leadId);

    @GetMapping
    ApiResponse<PageResponse<CustomerDto>> searchCustomers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String q);
}
