package com.aisales.common.contracts.client;

import com.aisales.common.contracts.search.AutocompleteRequest;
import com.aisales.common.contracts.search.SearchRequest;
import com.aisales.common.contracts.search.SearchResponse;
import com.aisales.common.core.dto.ApiResponse;
import java.util.List;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(
        name = "search-service",
        path = "/api/v1/search",
        url = "${aisales.clients.search-service.url:}")
public interface SearchServiceClient {

    @PostMapping
    ApiResponse<SearchResponse> search(@RequestBody SearchRequest request);

    @PostMapping("/autocomplete")
    ApiResponse<List<String>> autocomplete(@RequestBody AutocompleteRequest request);
}
