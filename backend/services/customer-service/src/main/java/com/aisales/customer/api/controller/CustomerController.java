package com.aisales.customer.api.controller;

import com.aisales.common.contracts.customer.AddContactMethodRequest;
import com.aisales.common.contracts.customer.ConvertLeadToCustomerRequest;
import com.aisales.common.contracts.customer.CreateCustomerAddressRequest;
import com.aisales.common.contracts.customer.CreateCustomerRequest;
import com.aisales.common.contracts.customer.CustomerAddressDto;
import com.aisales.common.contracts.customer.CustomerConsentDto;
import com.aisales.common.contracts.customer.CustomerContactMethodDto;
import com.aisales.common.contracts.customer.CustomerDto;
import com.aisales.common.contracts.customer.CustomerDuplicateDto;
import com.aisales.common.contracts.customer.CustomerInteractionDto;
import com.aisales.common.contracts.customer.CustomerStatus;
import com.aisales.common.contracts.customer.CustomerTimelineEntryDto;
import com.aisales.common.contracts.customer.IdentityResolutionResultDto;
import com.aisales.common.contracts.customer.LinkLeadRequest;
import com.aisales.common.contracts.customer.MergeCustomerRequest;
import com.aisales.common.contracts.customer.RecordConsentRequest;
import com.aisales.common.contracts.customer.RecordInteractionRequest;
import com.aisales.common.contracts.customer.ResolveIdentityRequest;
import com.aisales.common.contracts.customer.UpdateCustomerAddressRequest;
import com.aisales.common.contracts.customer.UpdateCustomerRequest;
import com.aisales.common.contracts.customer.UpdatePreferencesRequest;
import com.aisales.common.core.dto.ApiResponse;
import com.aisales.common.core.dto.PageResponse;
import com.aisales.common.core.constant.ApiConstants;
import com.aisales.common.security.annotation.PreAuthorizeTenant;
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
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/customers")
@RequiredArgsConstructor
@PreAuthorizeTenant
@Tag(name = "Customers", description = "Industry-agnostic customer aggregate with identity resolution")
public class CustomerController {

    private final CustomerService customerService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a customer")
    public ApiResponse<CustomerDto> create(
            @Valid @RequestBody CreateCustomerRequest request,
            @RequestHeader(value = ApiConstants.IDEMPOTENCY_KEY_HEADER, required = false)
                    String idempotencyKey) {
        return ApiResponse.ok(customerService.createCustomer(request, idempotencyKey));
    }

    @GetMapping
    @Operation(summary = "Search customers by name, phone, email, WhatsApp, external id, customer number")
    public ApiResponse<PageResponse<CustomerDto>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) CustomerStatus status,
            @RequestParam(required = false) String q) {
        return ApiResponse.ok(customerService.listCustomers(page, size, status, q));
    }

    @PostMapping("/identity/resolve")
    @Operation(summary = "Resolve customer identity (exact / probable / no match)")
    public ApiResponse<IdentityResolutionResultDto> resolveIdentity(
            @Valid @RequestBody ResolveIdentityRequest request) {
        return ApiResponse.ok(customerService.resolveIdentity(request));
    }

    @PostMapping("/from-lead")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Convert lead to customer or link an existing customer")
    public ApiResponse<CustomerDto> convertFromLead(
            @Valid @RequestBody ConvertLeadToCustomerRequest request) {
        return ApiResponse.ok(customerService.convertLeadToCustomer(request));
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
    @Operation(summary = "Update customer profile and contact details")
    public ApiResponse<CustomerDto> update(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateCustomerRequest request) {
        return ApiResponse.ok(customerService.updateCustomer(id, request));
    }

    @PutMapping("/{id}/preferences")
    @Operation(summary = "Update customer preferences")
    public ApiResponse<CustomerDto> updatePreferences(
            @PathVariable UUID id,
            @Valid @RequestBody UpdatePreferencesRequest request) {
        return ApiResponse.ok(customerService.updatePreferences(id, request));
    }

    @PostMapping("/{id}/activate")
    @Operation(summary = "Activate a customer")
    public ApiResponse<CustomerDto> activate(@PathVariable UUID id) {
        return ApiResponse.ok(customerService.activate(id));
    }

    @PostMapping("/{id}/deactivate")
    @Operation(summary = "Deactivate a customer")
    public ApiResponse<CustomerDto> deactivate(
            @PathVariable UUID id,
            @RequestBody(required = false) Map<String, String> body) {
        String reason = body == null ? null : body.get("reason");
        return ApiResponse.ok(customerService.deactivate(id, reason));
    }

    @PostMapping("/{id}/reactivate")
    @Operation(summary = "Reactivate an inactive or archived customer")
    public ApiResponse<CustomerDto> reactivate(@PathVariable UUID id) {
        return ApiResponse.ok(customerService.reactivate(id));
    }

    @PostMapping("/{id}/archive")
    @Operation(summary = "Archive a customer")
    public ApiResponse<CustomerDto> archive(
            @PathVariable UUID id,
            @RequestBody(required = false) Map<String, String> body) {
        String reason = body == null ? null : body.get("reason");
        return ApiResponse.ok(customerService.archive(id, reason));
    }

    @PostMapping("/{id}/link-lead")
    @Operation(summary = "Link an existing customer to a lead")
    public ApiResponse<CustomerDto> linkLead(
            @PathVariable UUID id,
            @Valid @RequestBody LinkLeadRequest request) {
        return ApiResponse.ok(customerService.linkLead(id, request));
    }

    @PostMapping("/{id}/merge")
    @Operation(summary = "Merge another customer into this survivor")
    public ApiResponse<CustomerDto> merge(
            @PathVariable UUID id,
            @Valid @RequestBody MergeCustomerRequest request) {
        return ApiResponse.ok(customerService.mergeCustomer(id, request));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Soft-delete a customer")
    public void delete(@PathVariable UUID id) {
        customerService.deleteCustomer(id);
    }

    @GetMapping("/{id}/timeline")
    @Operation(summary = "Customer timeline")
    public ApiResponse<List<CustomerTimelineEntryDto>> timeline(@PathVariable UUID id) {
        return ApiResponse.ok(customerService.timeline(id));
    }

    @PostMapping("/{id}/interactions")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Record a customer interaction")
    public ApiResponse<CustomerInteractionDto> recordInteraction(
            @PathVariable UUID id,
            @Valid @RequestBody RecordInteractionRequest request) {
        return ApiResponse.ok(customerService.recordInteraction(id, request));
    }

    @GetMapping("/{id}/interactions")
    @Operation(summary = "List customer interactions")
    public ApiResponse<List<CustomerInteractionDto>> listInteractions(@PathVariable UUID id) {
        return ApiResponse.ok(customerService.listInteractions(id));
    }

    @PostMapping("/{id}/contacts")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Add a contact method")
    public ApiResponse<CustomerContactMethodDto> addContact(
            @PathVariable UUID id,
            @Valid @RequestBody AddContactMethodRequest request) {
        return ApiResponse.ok(customerService.addContactMethod(id, request));
    }

    @GetMapping("/{id}/contacts")
    @Operation(summary = "List contact methods")
    public ApiResponse<List<CustomerContactMethodDto>> listContacts(@PathVariable UUID id) {
        return ApiResponse.ok(customerService.listContactMethods(id));
    }

    @PostMapping("/{id}/contacts/{contactId}/verify")
    @Operation(summary = "Verify a contact method")
    public ApiResponse<CustomerContactMethodDto> verifyContact(
            @PathVariable UUID id, @PathVariable UUID contactId) {
        return ApiResponse.ok(customerService.verifyContact(id, contactId));
    }

    @PostMapping("/{id}/consents")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Record consent grant or withdrawal")
    public ApiResponse<CustomerConsentDto> recordConsent(
            @PathVariable UUID id,
            @Valid @RequestBody RecordConsentRequest request) {
        return ApiResponse.ok(customerService.recordConsent(id, request));
    }

    @GetMapping("/{id}/consents")
    @Operation(summary = "List consents")
    public ApiResponse<List<CustomerConsentDto>> listConsents(@PathVariable UUID id) {
        return ApiResponse.ok(customerService.listConsents(id));
    }

    @GetMapping("/{id}/duplicates")
    @Operation(summary = "List open duplicate candidates")
    public ApiResponse<List<CustomerDuplicateDto>> listDuplicates(@PathVariable UUID id) {
        return ApiResponse.ok(customerService.listDuplicates(id));
    }

    @PostMapping("/{id}/duplicates/{duplicateId}/resolve")
    @Operation(summary = "Resolve duplicate (optional merge)")
    public ApiResponse<CustomerDuplicateDto> resolveDuplicate(
            @PathVariable UUID id,
            @PathVariable UUID duplicateId,
            @RequestParam(defaultValue = "false") boolean merge) {
        return ApiResponse.ok(customerService.resolveDuplicate(id, duplicateId, merge));
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
