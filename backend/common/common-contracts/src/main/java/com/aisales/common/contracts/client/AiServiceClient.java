package com.aisales.common.contracts.client;

import com.aisales.common.contracts.ai.AiExecuteRequest;
import com.aisales.common.contracts.ai.AiExecuteResponse;
import com.aisales.common.contracts.ai.CreateKnowledgeBaseRequest;
import com.aisales.common.contracts.ai.CreatePromptRequest;
import com.aisales.common.contracts.ai.CreatePromptVersionRequest;
import com.aisales.common.contracts.ai.KnowledgeBaseDto;
import com.aisales.common.contracts.ai.KnowledgeDocumentDto;
import com.aisales.common.contracts.ai.PromptDto;
import com.aisales.common.contracts.ai.PromptVersionDto;
import com.aisales.common.contracts.ai.RegisterKnowledgeDocumentRequest;
import com.aisales.common.core.dto.ApiResponse;
import com.aisales.common.core.dto.PageResponse;
import java.util.List;
import java.util.UUID;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(
        name = "ai-service",
        path = "/api/v1",
        url = "${aisales.clients.ai-service.url:}")
public interface AiServiceClient {

    @PostMapping("/prompts")
    ApiResponse<PromptDto> createPrompt(@RequestBody CreatePromptRequest request);

    @GetMapping("/prompts/{id}")
    ApiResponse<PromptDto> getPrompt(@PathVariable UUID id);

    @GetMapping("/prompts/by-code/{code}")
    ApiResponse<PromptDto> getPromptByCode(@PathVariable String code);

    @GetMapping("/prompts")
    ApiResponse<PageResponse<PromptDto>> listPrompts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size);

    @PostMapping("/prompts/{id}/versions")
    ApiResponse<PromptVersionDto> createPromptVersion(
            @PathVariable UUID id, @RequestBody CreatePromptVersionRequest request);

    @PostMapping("/ai/execute")
    ApiResponse<AiExecuteResponse> execute(@RequestBody AiExecuteRequest request);

    @PostMapping("/knowledge-bases")
    ApiResponse<KnowledgeBaseDto> createKnowledgeBase(@RequestBody CreateKnowledgeBaseRequest request);

    @GetMapping("/knowledge-bases/{id}")
    ApiResponse<KnowledgeBaseDto> getKnowledgeBase(@PathVariable UUID id);

    @PostMapping("/knowledge-bases/{id}/documents")
    ApiResponse<KnowledgeDocumentDto> registerDocument(
            @PathVariable UUID id, @RequestBody RegisterKnowledgeDocumentRequest request);

    @GetMapping("/knowledge-bases/{id}/documents")
    ApiResponse<List<KnowledgeDocumentDto>> listDocuments(@PathVariable UUID id);
}
