package com.aisales.conversation.api.controller;

import com.aisales.common.contracts.conversation.AddMessageRequest;
import com.aisales.common.contracts.conversation.ConversationDto;
import com.aisales.common.contracts.conversation.ConversationMessageDto;
import com.aisales.common.contracts.conversation.CreateConversationRequest;
import com.aisales.common.core.dto.ApiResponse;
import com.aisales.common.core.dto.PageResponse;
import com.aisales.conversation.application.service.ConversationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/conversations")
@RequiredArgsConstructor
@Tag(name = "Conversations", description = "Lead/customer conversation threads and messages")
public class ConversationController {

    private final ConversationService conversationService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Start a conversation")
    public ApiResponse<ConversationDto> start(@Valid @RequestBody CreateConversationRequest request) {
        return ApiResponse.ok(conversationService.start(request));
    }

    @GetMapping
    @Operation(summary = "List conversations (optionally by leadId)")
    public ApiResponse<PageResponse<ConversationDto>> list(
            @RequestParam(required = false) UUID leadId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(conversationService.list(leadId, page, size));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a conversation")
    public ApiResponse<ConversationDto> get(@PathVariable UUID id) {
        return ApiResponse.ok(conversationService.get(id));
    }

    @PostMapping("/{id}/messages")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Add a message")
    public ApiResponse<ConversationMessageDto> addMessage(
            @PathVariable UUID id,
            @Valid @RequestBody AddMessageRequest request) {
        return ApiResponse.ok(conversationService.addMessage(id, request));
    }

    @GetMapping("/{id}/messages")
    @Operation(summary = "List messages")
    public ApiResponse<List<ConversationMessageDto>> listMessages(@PathVariable UUID id) {
        return ApiResponse.ok(conversationService.listMessages(id));
    }

    @PostMapping("/{id}/close")
    @Operation(summary = "Close a conversation")
    public ApiResponse<ConversationDto> close(
            @PathVariable UUID id,
            @RequestBody(required = false) Map<String, String> body) {
        String reason = body == null ? null : body.get("reason");
        return ApiResponse.ok(conversationService.close(id, reason));
    }
}
