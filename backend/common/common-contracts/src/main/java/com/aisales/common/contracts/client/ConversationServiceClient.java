package com.aisales.common.contracts.client;

import com.aisales.common.contracts.conversation.AddMessageRequest;
import com.aisales.common.contracts.conversation.ConversationAiInsightsDto;
import com.aisales.common.contracts.conversation.ConversationContextDto;
import com.aisales.common.contracts.conversation.ConversationDto;
import com.aisales.common.contracts.conversation.ConversationMessageDto;
import com.aisales.common.contracts.conversation.CreateConversationRequest;
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
        name = "conversation-service",
        path = "/api/v1/conversations",
        url = "${aisales.clients.conversation-service.url:}")
public interface ConversationServiceClient {

    @PostMapping
    ApiResponse<ConversationDto> create(@RequestBody CreateConversationRequest request);

    @GetMapping("/{id}")
    ApiResponse<ConversationDto> get(@PathVariable UUID id);

    @GetMapping
    ApiResponse<PageResponse<ConversationDto>> listByLead(
            @RequestParam UUID leadId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size);

    @PostMapping("/{id}/messages")
    ApiResponse<ConversationMessageDto> addMessage(
            @PathVariable UUID id, @RequestBody AddMessageRequest request);

    @GetMapping("/{id}/messages")
    ApiResponse<List<ConversationMessageDto>> listMessages(@PathVariable UUID id);

    @GetMapping("/{id}/context")
    ApiResponse<ConversationContextDto> getContext(@PathVariable UUID id);

    @PostMapping("/{id}/ai/insights")
    ApiResponse<ConversationAiInsightsDto> generateInsights(@PathVariable UUID id);

    @PostMapping("/{id}/ai/reply")
    ApiResponse<ConversationMessageDto> suggestReply(@PathVariable UUID id);

    @PostMapping("/{id}/close")
    ApiResponse<ConversationDto> close(@PathVariable UUID id);
}
