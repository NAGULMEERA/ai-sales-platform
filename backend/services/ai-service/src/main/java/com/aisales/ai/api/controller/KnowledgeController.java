package com.aisales.ai.api.controller;

import com.aisales.ai.application.service.KnowledgeService;
import com.aisales.common.contracts.ai.CreateKnowledgeBaseRequest;
import com.aisales.common.contracts.ai.KnowledgeBaseDto;
import com.aisales.common.contracts.ai.KnowledgeDocumentDto;
import com.aisales.common.contracts.ai.RegisterKnowledgeDocumentRequest;
import com.aisales.common.core.dto.ApiResponse;
import com.aisales.common.core.dto.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Tag(name = "Knowledge", description = "Knowledge base metadata (binaries in media-service/S3)")
public class KnowledgeController {

    private final KnowledgeService knowledgeService;

    @PostMapping("/api/v1/knowledge-bases")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a knowledge base")
    public ApiResponse<KnowledgeBaseDto> createBase(@Valid @RequestBody CreateKnowledgeBaseRequest request) {
        return ApiResponse.ok(knowledgeService.createBase(request));
    }

    @GetMapping("/api/v1/knowledge-bases")
    @Operation(summary = "List knowledge bases")
    public ApiResponse<PageResponse<KnowledgeBaseDto>> listBases(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(knowledgeService.listBases(page, size));
    }

    @GetMapping("/api/v1/knowledge-bases/{id}")
    @Operation(summary = "Get a knowledge base")
    public ApiResponse<KnowledgeBaseDto> getBase(@PathVariable UUID id) {
        return ApiResponse.ok(knowledgeService.getBase(id));
    }

    @PostMapping("/api/v1/knowledge-bases/{id}/documents")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Register document metadata (no binary upload)")
    public ApiResponse<KnowledgeDocumentDto> registerDocument(
            @PathVariable UUID id, @Valid @RequestBody RegisterKnowledgeDocumentRequest request) {
        return ApiResponse.ok(knowledgeService.registerDocument(id, request));
    }

    @GetMapping("/api/v1/knowledge-bases/{id}/documents")
    @Operation(summary = "List documents in a knowledge base")
    public ApiResponse<List<KnowledgeDocumentDto>> listDocuments(@PathVariable UUID id) {
        return ApiResponse.ok(knowledgeService.listDocuments(id));
    }

    @GetMapping("/api/v1/knowledge-documents/{id}")
    @Operation(summary = "Get knowledge document metadata")
    public ApiResponse<KnowledgeDocumentDto> getDocument(@PathVariable UUID id) {
        return ApiResponse.ok(knowledgeService.getDocument(id));
    }
}
