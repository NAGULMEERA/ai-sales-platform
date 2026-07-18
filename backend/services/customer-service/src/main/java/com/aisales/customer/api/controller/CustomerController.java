package com.aisales.customer.api.controller;

import com.aisales.common.contracts.customer.CreateCustomerAddressRequest;
import com.aisales.common.contracts.customer.CreateCustomerRequest;
import com.aisales.common.contracts.customer.CustomerAddressDto;
import com.aisales.common.contracts.customer.CustomerDto;
import com.aisales.common.contracts.customer.CustomerStatus;
import com.aisales.common.contracts.customer.UpdateCustomerAddressRequest;
import com.aisales.common.contracts.customer.UpdateCustomerRequest;
import com.aisales.common.core.dto.ApiResponse;
import com.aisales.common.core.dto.PageResponse;
import com.aisales.customer.application.service.CustomerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/customers")
@RequiredArgsConstructor
@Tag(name = "Customers", description = "Industry-agnostic customer aggregate")
public class CustomerController {

    private final CustomerService customerService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a customer")
    public ApiResponse<CustomerDto> create(@Valid @RequestBody CreateCustomerRequest request) {
        return ApiResponse.ok(customerService.createCustomer(request));
    }

    @GetMapping
    @Operation(summary = "Search customers")
    public ApiResponse<PageResponse<CustomerDto>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) CustomerStatus status,
            @RequestParam(required = false) String q) {
        return ApiResponse.ok(customerService.listCustomers(page, size, status, q));
    }

    @GetMapping("/by-source-lead/{leadId}")
    @Operation(summary = "Get customer created from a lead conversion")
    public ApiResponse<CustomerDto> getBySourceLead(@PathVariable UUID leadId) {
        return ApiResponse.ok(customerService.getBySourceLead(leadId));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a customer")
    public ApiResponse<CustomerDto> get(@PathVariable UUID id) {
        return ApiResponse.ok(customerService.getCustomer(id));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a customer")
    public ApiResponse<CustomerDto> update(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateCustomerRequest request) {
        return ApiResponse.ok(customerService.updateCustomer(id, request));
    }

    @PostMapping("/{id}/activate")
    @Operation(summary = "Activate a customer")
    public ApiResponse<CustomerDto> activate(@PathVariable UUID id) {
        return ApiResponse.ok(customerService.activate(id));
    }

    @PostMapping("/{id}/archive")
    @Operation(summary = "Archive a customer")
    public ApiResponse<CustomerDto> archive(
            @PathVariable UUID id,
            @RequestBody(required = false) Map<String, String> body) {
        String reason = body == null ? null : body.get("reason");
        return ApiResponse.ok(customerService.archive(id, reason));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Soft-delete a customer")
    public void delete(@PathVariable UUID id) {
        customerService.deleteCustomer(id);
    }

    @PostMapping("/{id}/addresses")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Add a customer address")
    public ApiResponse<CustomerAddressDto> addAddress(
            @PathVariable UUID id,
            @Valid @RequestBody CreateCustomerAddressRequest request) {
        return ApiResponse.ok(customerService.addAddress(id, request));
    }

    @GetMapping("/{id}/addresses")
    @Operation(summary = "List customer addresses")
    public ApiResponse<List<CustomerAddressDto>> listAddresses(@PathVariable UUID id) {
        return ApiResponse.ok(customerService.listAddresses(id));
    }

    @PutMapping("/{id}/addresses/{addressId}")
    @Operation(summary = "Update a customer address")
    public ApiResponse<CustomerAddressDto> updateAddress(
            @PathVariable UUID id,
            @PathVariable UUID addressId,
            @Valid @RequestBody UpdateCustomerAddressRequest request) {
        return ApiResponse.ok(customerService.updateAddress(id, addressId, request));
    }

    @DeleteMapping("/{id}/addresses/{addressId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Soft-delete a customer address")
    public void deleteAddress(@PathVariable UUID id, @PathVariable UUID addressId) {
        customerService.deleteAddress(id, addressId);
    }
}
