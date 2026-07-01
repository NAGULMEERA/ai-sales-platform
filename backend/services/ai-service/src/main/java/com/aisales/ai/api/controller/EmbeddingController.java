package com.aisales.ai.api.controller;

import com.aisales.ai.api.request.EmbeddingRequest;
import com.aisales.ai.api.response.EmbeddingResponse;
import com.aisales.ai.application.service.EmbeddingApplicationService;
import com.aisales.common.core.constant.ApiConstants;
import com.aisales.common.security.annotation.PreAuthorizeTenant;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(ApiConstants.API_V1 + "/embeddings")
@RequiredArgsConstructor
public class EmbeddingController {

    private final EmbeddingApplicationService embeddingApplicationService;

    @PostMapping
    @PreAuthorizeTenant
    public ResponseEntity<EmbeddingResponse> embed(@Valid @RequestBody EmbeddingRequest request) {
        return ResponseEntity.ok(embeddingApplicationService.embed(request));
    }
}
