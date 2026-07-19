package com.aisales.search.api.controller;

import com.aisales.common.contracts.search.AutocompleteRequest;
import com.aisales.common.contracts.search.SearchEntityType;
import com.aisales.common.contracts.search.SearchMode;
import com.aisales.common.contracts.search.SearchRequest;
import com.aisales.common.contracts.search.SearchResponse;
import com.aisales.common.core.dto.ApiResponse;
import com.aisales.common.security.annotation.PreAuthorizeTenant;
import com.aisales.search.application.service.SearchQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/search")
@RequiredArgsConstructor
@PreAuthorizeTenant
@PreAuthorize("hasAuthority('search:read') or hasAuthority('tenant:admin') or hasAnyRole('TENANT_ADMIN','ADMIN','SUPER_ADMIN')")
@Tag(name = "Search", description = "Tenant-aware hybrid enterprise search")
public class SearchController {

    private final SearchQueryService searchQueryService;

    @PostMapping
    @Operation(summary = "Unified hybrid search")
    public ApiResponse<SearchResponse> search(@Valid @RequestBody SearchRequest request) {
        return ApiResponse.ok(searchQueryService.search(request));
    }

    @PostMapping("/global")
    @Operation(summary = "Global search across all entity types")
    public ApiResponse<SearchResponse> global(@Valid @RequestBody SearchRequest request) {
        request.setEntityType(SearchEntityType.ALL);
        if (request.getMode() == null) {
            request.setMode(SearchMode.HYBRID);
        }
        return ApiResponse.ok(searchQueryService.search(request));
    }

    @PostMapping("/leads")
    @Operation(summary = "Search leads")
    public ApiResponse<SearchResponse> searchLeads(@Valid @RequestBody SearchRequest request) {
        request.setEntityType(SearchEntityType.LEAD);
        return ApiResponse.ok(searchQueryService.search(request));
    }

    @PostMapping("/customers")
    @Operation(summary = "Search customers")
    public ApiResponse<SearchResponse> searchCustomers(@Valid @RequestBody SearchRequest request) {
        request.setEntityType(SearchEntityType.CUSTOMER);
        return ApiResponse.ok(searchQueryService.search(request));
    }

    @PostMapping("/catalog")
    @Operation(summary = "Search catalog products")
    public ApiResponse<SearchResponse> searchCatalog(@Valid @RequestBody SearchRequest request) {
        request.setEntityType(SearchEntityType.CATALOG);
        return ApiResponse.ok(searchQueryService.search(request));
    }

    @PostMapping("/opportunities")
    @Operation(summary = "Search opportunities")
    public ApiResponse<SearchResponse> searchOpportunities(@Valid @RequestBody SearchRequest request) {
        request.setEntityType(SearchEntityType.OPPORTUNITY);
        return ApiResponse.ok(searchQueryService.search(request));
    }

    @PostMapping("/conversations")
    @Operation(summary = "Search conversations")
    public ApiResponse<SearchResponse> searchConversations(@Valid @RequestBody SearchRequest request) {
        request.setEntityType(SearchEntityType.CONVERSATION);
        return ApiResponse.ok(searchQueryService.search(request));
    }

    @PostMapping("/semantic")
    @Operation(summary = "Semantic / knowledge search via AI Gateway RAG")
    public ApiResponse<SearchResponse> semantic(@Valid @RequestBody SearchRequest request) {
        request.setMode(SearchMode.SEMANTIC);
        request.setEntityType(SearchEntityType.KNOWLEDGE);
        return ApiResponse.ok(searchQueryService.search(request));
    }

    @PostMapping("/autocomplete")
    @Operation(summary = "Autocomplete titles")
    public ApiResponse<List<String>> autocomplete(@Valid @RequestBody AutocompleteRequest request) {
        return ApiResponse.ok(searchQueryService.autocomplete(request));
    }
}
